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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import io.github.msdk.datamodel.impl.SimpleChromatogram;
import io.github.msdk.datamodel.impl.SimpleFeature;

/**
 * <p>
 * This class is used to run the whole ADAP3D algorithm and get peaks.
 * </p>
 *
 */
public class ADAP3DAlgorithm {

  private SliceSparseMatrix objSliceSparseMatrix;

  private static final double LOW_BOUND_PEAK_WIDTH_PERCENT = 0.75;

  ADAP3DAlgorithm(SliceSparseMatrix sliceSparseMatrix) {
    objSliceSparseMatrix = sliceSparseMatrix;
  }

  /**
   * <p>
   * This method finds first 20 peaks from raw file and calculate parameters. Then it runs algorithm
   * with new parameters and find rest of the peaks. It returns the list of SimpleFeature which
   * includes chromatogram.
   * </p>
   * 
   * @return newFeatureList a list of {@link io.github.msdk.datamodel.impl.SimpleFeature}
   * 
   */
  public List<SimpleFeature> execute() {

    CurveTool objCurveTool = new CurveTool(objSliceSparseMatrix);
    double fwhm = objCurveTool.estimateFwhmMs();
    int roundedFWHM = objSliceSparseMatrix.roundMZ(fwhm);

    Parameters objParameters = new Parameters();

    PeakDetection objPeakDetection = new PeakDetection(objSliceSparseMatrix);
    List<PeakDetection.GoodPeakInfo> goodPeakList =
        objPeakDetection.execute(1000, objParameters, roundedFWHM);
    List<SimpleFeature> featureList = getFeature(goodPeakList);

    double[] peakWidth = new double[goodPeakList.size()];
    double avgCoefOverArea = 0.0;
    double avgPeakWidth = 0.0;

    for (int i = 0; i < goodPeakList.size(); i++) {
      peakWidth[i] = objSliceSparseMatrix.getRetentionTime(goodPeakList.get(i).upperScanBound) / 60
          - objSliceSparseMatrix.getRetentionTime(goodPeakList.get(i).lowerScanBound) / 60;
      avgPeakWidth += peakWidth[i];
      avgCoefOverArea += goodPeakList.get(i).objResult.coefOverArea;
    }

    avgPeakWidth = avgPeakWidth / goodPeakList.size();
    avgCoefOverArea = avgCoefOverArea / goodPeakList.size();

    int highestWaveletScale = (int) (avgPeakWidth * 60 / 2);
    double coefOverAreaThreshold = avgCoefOverArea / 1.5;


    List<Double> peakWidthList = Arrays.asList(ArrayUtils.toObject(peakWidth));
    double peakDurationLowerBound = avgPeakWidth - LOW_BOUND_PEAK_WIDTH_PERCENT * avgPeakWidth;

    double peakDurationUpperBound =
        Collections.max(peakWidthList) + LOW_BOUND_PEAK_WIDTH_PERCENT * avgPeakWidth;

    objParameters.setLargeScaleIn(highestWaveletScale);
    objParameters.setMinPeakWidth(peakDurationLowerBound);
    objParameters.setMaxPeakWidth(peakDurationUpperBound);
    objParameters.setCoefAreaRatioTolerance(coefOverAreaThreshold);


    List<PeakDetection.GoodPeakInfo> newGoodPeakList =
        objPeakDetection.execute(objParameters, roundedFWHM);
    List<SimpleFeature> newFeatureList = new ArrayList<SimpleFeature>();
    newFeatureList.addAll(featureList);
    newFeatureList.addAll(getFeature(newGoodPeakList));

    return newFeatureList;
  }



  /**
   * <p>
   * This method takes list of GoodPeakInfo and returns list of type SimpleFeature. This method also
   * builds Chromatogram for each good peak.
   * </p>
   * 
   * @return featureList a list of {@link io.github.msdk.datamodel.impl.SimpleFeature}
   */
  private List<SimpleFeature> getFeature(List<PeakDetection.GoodPeakInfo> goodPeakList) {

    List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();
    int lowerScanBound;
    int upperScanBound;

    for (int i = 0; i < goodPeakList.size(); i++) {

      lowerScanBound = goodPeakList.get(i).lowerScanBound;
      upperScanBound = goodPeakList.get(i).upperScanBound;
      double mz = goodPeakList.get(i).mz;
      float[] rtArray = objSliceSparseMatrix.getRetentionTimeArray(lowerScanBound, upperScanBound);
      float[] intensityArray = objSliceSparseMatrix.getIntensities(goodPeakList.get(i));
      double[] mzArray = new double[upperScanBound - lowerScanBound + 1];

      for (int j = 0; j < upperScanBound - lowerScanBound + 1; j++) {
        mzArray[j] = mz;
      }

      SimpleChromatogram chromatogram = new SimpleChromatogram();
      chromatogram.setDataPoints(rtArray, mzArray, intensityArray,
          upperScanBound - lowerScanBound + 1);

      SimpleFeature feature = new SimpleFeature();
      feature.setArea(CurveTool.normalize(intensityArray));
      feature.setHeight(goodPeakList.get(i).maxHeight);
      feature.setRetentionTime(
          (float) objSliceSparseMatrix.getRetentionTime(goodPeakList.get(i).maxHeightScanNumber));
      feature.setMz(mz);
      feature.setChromatogram(chromatogram);
      featureList.add(feature);
    }

    return featureList;

  }
}
