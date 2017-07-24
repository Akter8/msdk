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

package io.github.msdk.io.mzml2.data;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;

import io.github.msdk.MSDKRuntimeException;
import io.github.msdk.datamodel.chromatograms.Chromatogram;
import io.github.msdk.datamodel.chromatograms.ChromatogramType;
import io.github.msdk.datamodel.impl.SimpleActivationInfo;
import io.github.msdk.datamodel.impl.SimpleIsolationInfo;
import io.github.msdk.datamodel.ionannotations.IonAnnotation;
import io.github.msdk.datamodel.rawdata.ActivationInfo;
import io.github.msdk.datamodel.rawdata.ActivationType;
import io.github.msdk.datamodel.rawdata.IsolationInfo;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.datamodel.rawdata.SeparationType;
import io.github.msdk.io.mzml2.MzMLFileImportMethod;

class MzMLChromatogram implements Chromatogram {

  private final @Nonnull MzMLRawDataFile dataFile;
  private @Nonnull InputStream inputStream;
  private final @Nonnull String chromatogramId;
  private final @Nonnull Integer chromatogramNumber;
  private final @Nonnull Integer numOfDataPoints;

  private ArrayList<MzMLCVParam> cvParams;
  private MzMLPrecursorElement precursor;
  private MzMLProduct product;
  private MzMLBinaryDataInfo rtBinaryDataInfo;
  private MzMLBinaryDataInfo intensityBinaryDataInfo;
  private ChromatogramType chromatogramType;
  private Double mz;
  private SeparationType separationType;
  private Range<Float> rtRange;
  private float[] rtValues;
  private float[] intensityValues;

  private Logger logger = LoggerFactory.getLogger(MzMLFileImportMethod.class);

  MzMLChromatogram(@Nonnull MzMLRawDataFile dataFile, InputStream is, String chromatogramId,
      Integer chromatogramNumber, Integer numOfDataPoints) {
    this.cvParams = new ArrayList<>();
    this.dataFile = dataFile;
    this.inputStream = is;
    this.chromatogramId = chromatogramId;
    this.chromatogramNumber = chromatogramNumber;
    this.numOfDataPoints = numOfDataPoints;
    this.separationType = SeparationType.UNKNOWN;
    this.rtBinaryDataInfo = null;
    this.intensityBinaryDataInfo = null;
    this.chromatogramType = ChromatogramType.UNKNOWN;
    this.mz = null;
    this.rtRange = null;
    this.rtValues = null;
    this.intensityValues = null;

  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public RawDataFile getRawDataFile() {
    return dataFile;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public Integer getChromatogramNumber() {
    return chromatogramNumber;
  }

  /**
   * <p>
   * getCVParams.
   * </p>
   *
   * @return a {@link java.util.ArrayList} object.
   */
  public ArrayList<MzMLCVParam> getCVParams() {
    return cvParams;
  }

  /**
   * <p>
   * Getter for the field <code>mzBinaryDataInfo</code>.
   * </p>
   *
   * @return a {@link io.github.msdk.io.mzml2.data.MzMLBinaryDataInfo} object.
   */
  public MzMLBinaryDataInfo getRtBinaryDataInfo() {
    return rtBinaryDataInfo;
  }

  /**
   * <p>
   * Setter for the field <code>mzBinaryDataInfo</code>.
   * </p>
   *
   * @param rtBinaryDataInfo a {@link io.github.msdk.io.mzml2.data.MzMLBinaryDataInfo} object.
   */
  public void setRtBinaryDataInfo(MzMLBinaryDataInfo rtBinaryDataInfo) {
    this.rtBinaryDataInfo = rtBinaryDataInfo;
  }

  /**
   * <p>
   * Getter for the field <code>intensityBinaryDataInfo</code>.
   * </p>
   *
   * @return a {@link io.github.msdk.io.mzml2.data.MzMLBinaryDataInfo} object.
   */
  public MzMLBinaryDataInfo getIntensityBinaryDataInfo() {
    return intensityBinaryDataInfo;
  }

  /**
   * <p>
   * Setter for the field <code>intensityBinaryDataInfo</code>.
   * </p>
   *
   * @param intensityBinaryDataInfo a {@link io.github.msdk.io.mzml2.data.MzMLBinaryDataInfo}
   *        object.
   */
  public void setIntensityBinaryDataInfo(MzMLBinaryDataInfo intensityBinaryDataInfo) {
    this.intensityBinaryDataInfo = intensityBinaryDataInfo;
  }

  /**
   * <p>
   * getInputStream.
   * </p>
   *
   * @return a {@link io.github.msdk.io.mzml2.util.io.ByteBufferInputStream} object.
   */
  public InputStream getInputStream() {
    return inputStream;
  }

  /**
   * <p>
   * setInputStream.
   * </p>
   *
   * @param inputStream a {@link java.io.InputStream} object.
   */
  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * <p>
   * getPrecursor.
   * </p>
   *
   * @return a {@link io.github.msdk.io.mzml2.data.MzMLPrecursorElement} object.
   */
  public MzMLPrecursorElement getPrecursor() {
    return precursor;
  }

  /**
   * <p>
   * Setter for the field <code>precursor</code>.
   * </p>
   *
   * @param precursor a {@link io.github.msdk.io.mzml2.data.MzMLPrecursorElement} object.
   */
  public void setPrecursor(MzMLPrecursorElement precursor) {
    this.precursor = precursor;
  }

  /**
   * <p>
   * getProduct.
   * </p>
   *
   * @return a {@link io.github.msdk.io.mzml2.data.MzMLProduct} object.
   */
  public MzMLProduct getProduct() {
    return product;
  }

  /**
   * <p>
   * Setter for the field <code>precursor</code>.
   * </p>
   *
   * @param product a {@link io.github.msdk.io.mzml2.data.MzMLProduct} object.
   */
  public void setProdcut(MzMLProduct product) {
    this.product = product;
  }

  /**
   * <p>
   * Getter for the field <code>chromatogramId</code>.
   * </p>
   *
   * @return a {@link java.lang.String} object.
   */
  public String getId() {
    return chromatogramId;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public ChromatogramType getChromatogramType() {
    if (chromatogramType != ChromatogramType.UNKNOWN)
      return chromatogramType;

    int count = 0;

    if (getCVValue(MzMLCV.cvChromatogramTIC).isPresent()) {
      chromatogramType = ChromatogramType.TIC;
      count++;
    }
    if (getCVValue(MzMLCV.cvChromatogramMRM_SRM).isPresent()) {
      chromatogramType = ChromatogramType.MRM_SRM;
      count++;
    }
    if (getCVValue(MzMLCV.cvChromatogramSIC).isPresent()) {
      chromatogramType = ChromatogramType.SIC;
      count++;
    }
    if (getCVValue(MzMLCV.cvChromatogramBPC).isPresent()) {
      chromatogramType = ChromatogramType.BPC;
      count++;
    }

    if (count > 1) {
      logger.error("More than one chromatogram type defined by CV terms not allowed");
      chromatogramType = ChromatogramType.UNKNOWN;
    }

    return chromatogramType;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Double getMz() {
    // Set mz value only if it hasn't been fetched before
    if (mz == null) {
      // Try getting the mz value from the product isolation window
      // set mz value to the respective cv value only if the value is present
      if (product != null) {
        if (product.getIsolationWindow().isPresent()) {
          Optional<String> cvval =
              getCVValue(product.getIsolationWindow().get(), MzMLCV.cvIsolationWindowTarget);
          if (cvval.isPresent())
            mz = Double.valueOf(cvval.get());
        }
      }
    }
    return mz;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public List<IsolationInfo> getIsolations() {
    if (getChromatogramType() == ChromatogramType.MRM_SRM) {

      Optional<MzMLIsolationWindow> precursorIsolationWindow = getPrecursor().getIsolationWindow();
      Optional<MzMLIsolationWindow> productIsolationWindow = getProduct().getIsolationWindow();

      if (!precursorIsolationWindow.isPresent()) {
        logger.error("Couldn't find precursor isolation window for chromotgram (#"
            + getChromatogramNumber() + ")");
        return Collections.emptyList();
      }

      if (!productIsolationWindow.isPresent()) {
        logger.error("Couldn't find product isolation window for chromotgram (#"
            + getChromatogramNumber() + ")");
        return Collections.emptyList();
      }

      Optional<String> precursorIsolationMz =
          getCVValue(precursorIsolationWindow.get(), MzMLCV.cvIsolationWindowTarget);
      Optional<String> precursorActivationEnergy =
          getCVValue(getPrecursor().getActivation(), MzMLCV.cvActivationEnergy);
      Optional<String> productIsolationMz =
          getCVValue(productIsolationWindow.get(), MzMLCV.cvIsolationWindowTarget);
      ActivationType precursorActivation = ActivationType.UNKNOWN;
      ActivationInfo activationInfo = null;

      if (getCVValue(getPrecursor().getActivation(), MzMLCV.cvActivationCID).isPresent())
        precursorActivation = ActivationType.CID;

      if (precursorActivationEnergy != null) {
        activationInfo = new SimpleActivationInfo(Double.valueOf(precursorActivationEnergy.get()),
            precursorActivation);
      }

      List<IsolationInfo> isolations = new ArrayList<>();
      IsolationInfo isolationInfo = null;

      if (precursorIsolationMz.isPresent()) {
        isolationInfo =
            new SimpleIsolationInfo(Range.singleton(Double.valueOf(precursorIsolationMz.get())),
                null, Double.valueOf(precursorIsolationMz.get()), null, activationInfo);
        isolations.add(isolationInfo);
      }

      if (productIsolationMz.isPresent()) {
        isolationInfo =
            new SimpleIsolationInfo(Range.singleton(Double.valueOf(productIsolationMz.get())), null,
                Double.valueOf(productIsolationMz.get()), null, null);
        isolations.add(isolationInfo);
      }

      return Collections.unmodifiableList(isolations);
    }

    return Collections.emptyList();
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public SeparationType getSeparationType() {
    return separationType;
  }

  /** {@inheritDoc} */
  @Override
  public IonAnnotation getIonAnnotation() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public Integer getNumberOfDataPoints() {
    return numOfDataPoints;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public float[] getRetentionTimes(@Nullable float array[]) {
    if (rtValues == null) {
      if (getRtBinaryDataInfo().getArrayLength() != numOfDataPoints) {
        logger.warn(
            "Retention time binary data array contains a different array length from the default array length of the scan (#"
                + getChromatogramNumber() + ")");
      }

      try {
        rtValues = MzMLPeaksDecoder.decodeToFloat(inputStream, getRtBinaryDataInfo(), array);
      } catch (Exception e) {
        throw (new MSDKRuntimeException(e));
      }
    }

    return rtValues;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public float[] getIntensityValues(@Nullable float[] array) {
    if (intensityValues == null) {
      if (getIntensityBinaryDataInfo().getArrayLength() != numOfDataPoints) {
        logger.warn(
            "Intensity binary data array contains a different array length from the default array length of the chromatogram (#"
                + getChromatogramNumber() + ")");
      }

      try {
        intensityValues =
            MzMLPeaksDecoder.decodeToFloat(inputStream, getIntensityBinaryDataInfo(), array);
      } catch (Exception e) {
        throw (new MSDKRuntimeException(e));
      }
    }

    return intensityValues;

  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public double[] getMzValues(@Nullable double array[]) {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Range<Float> getRtRange() {
    if (rtRange == null) {
      float[] rtValues = getRetentionTimes();
      rtRange = Range.closed(rtValues[0], rtValues[numOfDataPoints - 1]);
    }
    return rtRange;
  }

  /**
   * <p>
   * getCVValue.
   * </p>
   *
   * @param accession a {@link java.lang.String} object.
   * @return a {@link java.lang.String} object.
   */
  public Optional<String> getCVValue(String accession) {
    for (MzMLCVParam cvParam : cvParams) {
      Optional<String> value;
      if (cvParam.getAccession().equals(accession)) {
        value = cvParam.getValue();
        if (!value.isPresent())
          value = Optional.of("");
        return value;
      }
    }
    return Optional.empty();
  }

  /**
   * <p>
   * getCVValue.
   * </p>
   *
   * @param group a {@link io.github.msdk.io.mzml2.data.MzMLCVGroup} object.
   * @param accession a {@link java.lang.String} object.
   * @return a {@link java.lang.String} object.
   */
  public Optional<String> getCVValue(MzMLCVGroup group, String accession) {
    Optional<String> value;
    for (MzMLCVParam cvParam : group.getCVParamsList()) {
      if (cvParam.getAccession().equals(accession)) {
        value = cvParam.getValue();
        if (!value.isPresent())
          value = Optional.of("");
        return value;
      }
    }
    return Optional.empty();
  }

}
