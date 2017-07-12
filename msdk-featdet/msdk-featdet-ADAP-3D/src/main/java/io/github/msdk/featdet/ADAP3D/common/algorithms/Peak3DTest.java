/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */
package io.github.msdk.featdet.ADAP3D.common.algorithms;

import org.apache.commons.collections4.map.MultiKeyMap;

import io.github.msdk.featdet.ADAP3D.common.algorithms.SliceSparseMatrix.Triplet;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Peak3DTest is used for determining true or false peak by comparing adjacent m/z-slices in profile
 * data.
 * </p>
 */
public class Peak3DTest {

  /**
   * <p>
   * Result class is used for returning lower and upper mz bound,boolean good peak value and list of
   * similarity value. Object of this class will return lowest mz boundary and highest mz boundary
   * of adjacent similar peaks for given mz value. Here lowerMzBound and upperMzBound are integer
   * because for sparse matrix we've rounded the mz value by multiplying 10000. It will also return
   * if the peak is good or not for given mz value.
   * </p>
   */
  public static class Result {
    List<Double> similarityValues;
    boolean goodPeak;
    int lowerMzBound;
    int upperMzBound;
  }

  enum Direction {
    UP, DOWN
  }

  /* Instance of SliceSparseMatrix containing the profile data */
  private final SliceSparseMatrix objsliceSparseMatrix;

  /* Full-Width Half-Max of m/z-profiles */
  private final double fwhm;

  public Peak3DTest(SliceSparseMatrix objsliceSparseMatrix, double fwhm) {
    this.objsliceSparseMatrix = objsliceSparseMatrix;
    this.fwhm = fwhm;
  }

  /**
   * <p>
   * execute method is used for testing a peak with given m/z-value (variable mz) and left and right
   * bounds (variables leftBound and rightBound).
   *
   * Peak is tested by comparing similarity between adjacent m/z slices.
   *
   * Let mzValues be a sorted list of all m/z values in the profile data. Let index be an integer
   * such that
   *
   * mzValues[index] == mz
   *
   * We find similarities between the EIC corresponding to m/z value mzValues[index] and adjacent
   * EICs corresponding to
   *
   * ..., mzValues[index-2], mzValues[index-1], mzValues[index+1], mzValues[index+2], ...
   *
   * as long as those similarities are higher than the similarity threshold. First, we check each
   * m/z-value higher than mzValues[index], stop when the current similarity becomes lower than the
   * similarity threshold, and save the last m/z-value (variable upperMZbound). Next, we check each
   * m/z-value lower than mzValues[index], stop when the current similarity becomes lower than the
   * similarity threshold, and save the last m/z-value (variable lowerMZbound)
   *
   * Peak is considered to be good if the differences upperMZbound - mzValues[index],
   * mzValues[index] - lowerMZbound, upperMZbound - lowerMZbound exceed certain thresholds, which
   * depend on FWHM-value.
   *
   *
   * @param mz a {@link java.lang.Double} object. It's double because original m/z value from raw
   *        file is passed in the method.
   * @param leftBound a {@link java.lang.Integer} object. This is lowest scan number from which peak
   *        determining starts.
   * @param rightBound a {@link java.lang.Integer} object. This is highest scan number on which peak
   *        determining ends.
   * 
   * @return a {@link Result} object. Result object contains similarity values, lower and upper mz
   *         boundaries for adjacent similar peaks.
   *         </p>
   */
  public Result execute(double mz, int leftBound, int rightBound) {

    // Here I'm rounding Full width half max(fwhm) and mz value by factor of roundMZfactor.
    // For instance, roundedMz = (int) mz * 10000
    int roundedFWHM = objsliceSparseMatrix.roundMZ(fwhm);
    int roundedMz = objsliceSparseMatrix.roundMZ(mz);

    // slice is used to store horizontal row from sparse matrix for given mz, left boundary and
    // right boundary.
    // left boundary and right boundary are used in form of scan numbers.
    MultiKeyMap<Integer, Triplet> slice =
        objsliceSparseMatrix.getHorizontalSlice(mz, leftBound, rightBound);

    // referenceEIC is used for storing normalized intensities for m/z-value equal to mz.
    double[] referenceEIC = new double[rightBound - leftBound + 1];
    // normalize method is used for normalizing intensities for given mz value. It updates
    // referenceEIC.
    normalize(slice, leftBound, rightBound, roundedMz, referenceEIC);


    // We save all similarity values to this list
    List<Double> similarityValues = new ArrayList<Double>();

    // mzIndex is index of given mz from the sorted list of all mz values from raw file.
    int mzIndex = objsliceSparseMatrix.mzValues.indexOf(roundedMz);

    // Here we're getting highest mz value for which the EIC is similar to given mz value.
    int upperMzBound = findMZbound(leftBound, rightBound, roundedMz, roundedFWHM, mzIndex,
        referenceEIC, similarityValues, Direction.UP);

    // Here we're getting lowest mz value for which the EIC is similar to given mz value.
    int lowerMzBound = findMZbound(leftBound, rightBound, roundedMz, roundedFWHM, mzIndex,
        referenceEIC, similarityValues, Direction.DOWN);

    // Assigning values to object.
    Result objResult = new Result();
    objResult.similarityValues = similarityValues;
    objResult.lowerMzBound = lowerMzBound;
    objResult.upperMzBound = upperMzBound;

    int lowerBoundaryDiff = roundedMz - lowerMzBound;
    int upperBoundaryDiff = upperMzBound - roundedMz;

    // This is the condition for determing whether the peak is good or not.
    if ((upperBoundaryDiff >= fwhm / 2) && (lowerBoundaryDiff >= fwhm / 2)
        && (upperBoundaryDiff + lowerBoundaryDiff >= fwhm)) {
      objResult.goodPeak = true;
    } else {
      objResult.goodPeak = false;
    }

    return objResult;

  }

  /**
   * <p>
   * findMZbound method is used to compare adjacent EICs to the reference EIC and return the last
   * m/z value such that its corresponding EIC is similar to the reference EIC.
   *
   * The similarity is calculated between two EICs in three steps: 1. Normalize EIC 2. Find area of
   * the difference of the normalized EICs (variable diffArea) 3. Calculate similarity by the
   * formula
   *
   * similarity = height * (exp(-diffArea / factor) - shift)
   *
   * @param roundedMz a {@link java.lang.Integer} object. This is m/z value which is multiplied by
   *        10000 because of it's use in sparse matrix.
   * @param leftBound a {@link java.lang.Integer} object. This is lowest scan number from which peak
   *        determining starts.
   * @param rightBound a {@link java.lang.Integer} object. This is highest scan number on which peak
   *        determining ends.
   * @param roundedFWHM a {@link java.lang.Double} object. fwhm is also multiplied by 10000 as m/z
   *        is multiplied by same.
   * @param mzIndex a {@link java.lang.Integer} object. This is the index of given m/z value in
   *        sorted list of all m/z values.
   * @param referenceEIC a {@link java.lang.Double} array. This array contains normalize intensities
   *        for given m/z value.(Intensities/area)
   * @param similarityValues a {@link java.lang.Double} empty list. This empty list stores
   *        similarity values.
   * @param direction a {@link Enum} object. This enum provides direction whether to call function
   *        for m/z values greater than given m/z or less than given m/z.
   * 
   * @return curMZ a {@link java.lang.Double} object. This is m/z value greater or less than given
   *         m/z value which is used for finding similar peaks.
   *         </p>
   */
  private int findMZbound(int leftBound, int rightBound, int roundedMz, double roundedFWHM,
      int mzIndex, double[] referenceEIC, List<Double> similarityValues, Direction direction) {

    final double exponentFactor = 0.2;
    final double exponentShift = Math.exp(-1 / exponentFactor);
    final double exponentHeight = 1.0 / (1.0 - exponentShift);

    final double peakSimilarityThreshold = 0.7;
    final double epsilon = 1E-8;

    final int multiplier = direction == Direction.UP ? 1 : -1;
    final int arrayCount = rightBound - leftBound + 1;

    Integer curMZ = null;



    int curMzIndex = 0;

    double curSimilarity = 1.0;
    int curInc = 0;

    while (curSimilarity > peakSimilarityThreshold) {

      curInc += 1;

      // This condition is used to determine whether we're finding similar peaks for mz values lower
      // or upper.
      // than given mz value.curMzIndex maintains index of cur mz in sorted mz value list.
      curMzIndex = mzIndex + curInc * multiplier;

      // This condition checks whether we've mz values above or below given mz value.

      curMZ = objsliceSparseMatrix.mzValues.get(curMzIndex);

      if (curMZ == null || Math.abs(curMZ - roundedMz) >= 2 * roundedFWHM)
        break;

      // //for getting slice of sparse matrix we need to provide original mz values which are there
      // in raw file.
      // double originalCurMZ = (double) curMZ / objsliceSparseMatrix.roundMzFactor;
      // curEIC will store normalized intensities for adjacent mz values.
      double[] curEIC = new double[arrayCount];

      // Here current horizontal slice from sparse matrix is stored adjacent mz value.
      MultiKeyMap<Integer, Triplet> curSlice =
          objsliceSparseMatrix.getHorizontalSlice(curMZ, leftBound, rightBound);
      double area = normalize(curSlice, leftBound, rightBound, curMZ, curEIC);

      // if area is too small continue.
      if (area < epsilon)
        continue;

      double diffArea = 0.0;
      // This is the implementation of trapezoid.
      for (int j = 0; j < arrayCount - 1; j++) {
        diffArea +=
            Math.abs(0.5 * ((referenceEIC[j] - curEIC[j]) + (referenceEIC[j + 1] - curEIC[j + 1])));
      }

      // Here similarity value is calculated.
      curSimilarity = ((Math.exp(-diffArea / exponentFactor)) - exponentShift) * exponentHeight;

      if (curSimilarity > peakSimilarityThreshold)
        similarityValues.add(curSimilarity);
    }

    curMZ = curMZ == null ? roundedMz : curMZ;
    return curMZ;
  }

  /**
   * <p>
   * normalize method is used for normalizing EIC by calculating its area and dividing each
   * intensity by the area.
   * 
   * @param roundedMz a {@link java.lang.Integer} object.This is m/z value which is multiplied by
   *        10000 because of it's use in sparse matrix.
   * @param leftBound a {@link java.lang.Integer} object. This is lowest scan number from which peak
   *        determining starts.
   * @param rightBound a {@link java.lang.Integer} object. This is highest scan number on which peak
   *        determining ends.
   * @param referenceEIC a {@link java.lang.Double} array. This array contains normalize intensities
   *        for given m/z value.(Intensities/area)
   * 
   * @return area a {@link java.lang.Double} object. This is area of normalize intensity points.
   *         </p>
   */
  private double normalize(MultiKeyMap<Integer, Triplet> slice, int leftBound, int rightBound,
      int roundedMz, double[] referenceEIC) {

    double area = 0.0;

    // Here area has been calculated for normalizing the intensities.
    for (int i = leftBound; i < rightBound; i++) {
      SliceSparseMatrix.Triplet obj1 = slice.get(i, roundedMz);
      SliceSparseMatrix.Triplet obj2 = slice.get(i + 1, roundedMz);
      double intensity1 = obj1 == null ? 0.0 : obj1.intensity;
      double intensity2 = obj2 == null ? 0.0 : obj2.intensity;
      area += 0.5 * (intensity1 + intensity2);
    }

    final int arrayCount = rightBound - leftBound + 1;

    // Here intensities are normalized.
    for (int i = 0; i < arrayCount; i++) {
      SliceSparseMatrix.Triplet triplet =
          (SliceSparseMatrix.Triplet) slice.get(i + leftBound, roundedMz);

      if (triplet != null) {
        referenceEIC[i] = triplet.intensity / (float) area;
      } else {
        referenceEIC[i] = 0;
      }
    }

    return area;
  }
}
