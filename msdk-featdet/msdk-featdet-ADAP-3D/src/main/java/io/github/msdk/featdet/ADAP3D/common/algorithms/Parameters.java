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

public class Parameters {

  private double peakSimilarityThreshold = 0.5;
  private double biGaussianSimilarityThreshold = 0.25;
  private int largeScaleIn = 10;
  private int delta = largeScaleIn * 5;
  private double coefAreaRatioTolerance = 100;
  private double minPeakWidth = 0.0;
  private double maxPeakWidth = 10.0;

  public void setPeakSimilarityThreshold(double thresholdValue) {
    peakSimilarityThreshold = thresholdValue;
  }

  public double getPeakSimilarityThreshold() {
    return peakSimilarityThreshold;
  }

  public void setBiGaussianSimilarityThreshold(double thresholdValue) {
    biGaussianSimilarityThreshold = thresholdValue;
  }

  public double getBiGaussianSimilarityThreshold() {
    return biGaussianSimilarityThreshold;
  }

  public void setDelta(int scale) {
    largeScaleIn = scale;
    delta = largeScaleIn * 5;

  }

  public int getDelta() {
    return delta;
  }

  public void setLargeScaleIn(int largeScale) {
    largeScaleIn = largeScale;
  }

  public int getLargeScaleIn() {
    return largeScaleIn;
  }

  public void setCoefAreaRatioTolerance(double coefOverAreaThreshold) {
    coefAreaRatioTolerance = coefOverAreaThreshold;
  }

  public double getCoefAreaRatioTolerance() {
    return coefAreaRatioTolerance;
  }

  public void setMinPeakWidth(double peakWidth) {
    minPeakWidth = peakWidth;
  }

  public double getMinPeakWidth() {
    return minPeakWidth;
  }

  public void setMaxPeakWidth(double peakWidth) {
    maxPeakWidth = peakWidth;
  }

  public double getMaxPeakWidth() {
    return maxPeakWidth;
  }
}
