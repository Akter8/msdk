/* 
 * (C) Copyright 2015 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */

package io.github.msdk.featuredetection.msmsdetection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.datapointstore.DataPointStore;
import io.github.msdk.datamodel.impl.MSDKObjectBuilder;
import io.github.msdk.datamodel.ionannotations.IonAnnotation;
import io.github.msdk.datamodel.msspectra.MsSpectrumDataPointList;
import io.github.msdk.datamodel.rawdata.ChromatographyInfo;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.datamodel.rawdata.SeparationType;
import io.github.msdk.util.MZTolerance;
import io.github.msdk.util.RTTolerance;

/**
 * This class creates a list of IonAnnotations for a RawDataFile based the
 * MS2 scans.
 */
public class MsMsDetectionMethod implements MSDKMethod<List<IonAnnotation>> {

    private final @Nonnull RawDataFile rawDataFile;
    private final @Nonnull List<MsScan> msScans;
    private final @Nonnull DataPointStore dataPointStore;
    private final @Nonnull MZTolerance mzTolerance;
    private final @Nonnull RTTolerance rtTolerance;
    private final @Nonnull Double intensityTolerance;

    private List<IonAnnotation> result;
    private boolean canceled = false;
    private int processedScans = 0, totalScans = 0;

    /**
     * <p>Constructor for MsMsDetectionMethod.</p>
     *
     * @param rawDataFile a {@link io.github.msdk.datamodel.rawdata.RawDataFile} object.
     * @param dataPointStore a {@link io.github.msdk.datamodel.datapointstore.DataPointStore} object.
     * @param mzTolerance a {@link io.github.msdk.util.MZTolerance} object.
     * @param rtTolerance a {@link io.github.msdk.util.RTTolerance} object.
     * @param intensityTolerance a {@link java.lang.Double} object.
     */
    public MsMsDetectionMethod(@Nonnull RawDataFile rawDataFile,
    		@Nonnull List<MsScan> msScans,
            @Nonnull DataPointStore dataPointStore,
            @Nonnull MZTolerance mzTolerance, @Nonnull RTTolerance rtTolerance,
            @Nonnull Double intensityTolerance) {
        this.rawDataFile = rawDataFile;
        this.msScans = msScans;
        this.dataPointStore = dataPointStore;
        this.mzTolerance = mzTolerance;
        this.rtTolerance = rtTolerance;
        this.intensityTolerance = intensityTolerance;
    }

    /** {@inheritDoc} */
    @Override
    public List<IonAnnotation> execute() throws MSDKException {

        result = new ArrayList<IonAnnotation>();
        totalScans = msScans.size();

        // No MS/MS scans found
        if (totalScans == 0)
        	return result;

        // Create a new scan data array
        double[][] scanData = new double[totalScans][3];

        // Loop through all MS/MS scans
        for (MsScan scan : msScans) {

        	// Calculate total intensity of the ions in the MS/MS spectrum
            MsSpectrumDataPointList dataPoints = MSDKObjectBuilder.getMsSpectrumDataPointList();
        	scan.getDataPoints(dataPoints);
        	float[] intensityBuffer = dataPoints.getIntensityBuffer();
        	double totalInteisity = 0;
        	for (int i = 0 ; i< dataPoints.getSize(); i++) {
        		totalInteisity = totalInteisity+ intensityBuffer[i];
        	}

        	// Selected ion m/z for the MS/MS scan (Precursor ion)
        	/*
        	 * TODO: Find the precursor ion for the scan
        	 */
        	double selectedMz = 0d;

        	// RT value
        	double scanRt = scan.getChromatographyInfo().getRetentionTime();

        	// Add the data to the array
        	scanData[processedScans][0] = selectedMz;
        	scanData[processedScans][1] = scanRt;
        	scanData[processedScans][2] = totalInteisity;

            if (canceled)
                return null;

        	processedScans++;
        }

        // Sort the array descending based on total intensity
        Arrays.sort(scanData, new Comparator<double[]>() {
            @Override
            public int compare(final double[] entry1, final double[] entry2) {
                final Double value1 = entry1[2];
                final Double value2 = entry2[2];
                return value2.compareTo(value1);
            }
        });

        // Loop through the array and find duplicates within the m/z and RT tolerances
        List<Integer> removeEntries = new ArrayList<>();
        for (int firstIndex = 0; firstIndex < scanData.length; firstIndex++) {

        	double floatMz1 =  scanData[firstIndex][0];
        	double floatRt1 =  scanData[firstIndex][1];

			// Loop through all the entries with lower intensity
			for (int secondIndex = firstIndex + 1; secondIndex < scanData.length; secondIndex++) {

				if (removeEntries.contains(secondIndex))
					continue;

				double floatMz2 =  scanData[secondIndex][0];
				double floatRt2 =  scanData[secondIndex][1];

				// Compare m/z
				final boolean sameMz = mzTolerance.getToleranceRange(floatMz1).contains(floatMz2);

				// Compare RT
				final boolean sameRt = rtTolerance.getToleranceRange(floatRt1).contains(floatRt2);

				// Same feature?
				if (sameMz && sameRt) {
					removeEntries.add(secondIndex);
				}
			}

        }

        // Add the unique entries to the result list
        for (int i = 0; i < scanData.length; i++) {
        	if (!removeEntries.contains(i)) {
            	double mzValue = scanData[i][0];
            	float rtValue = (float) scanData[i][1];

            	// Create ion
        		IonAnnotation ionAnnotation = MSDKObjectBuilder.getSimpleIonAnnotation();
        		ionAnnotation.setExpectedMz(mzValue);
        		ChromatographyInfo chromatographyInfo = MSDKObjectBuilder.getChromatographyInfo1D(SeparationType.UNKNOWN, rtValue);
        		ionAnnotation.setChromatographyInfo(chromatographyInfo);

        		// add the ion to the result
        		result.add(ionAnnotation);
        	}
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public Float getFinishedPercentage() {
        return totalScans == 0 ? null : (float) processedScans / totalScans;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public List<IonAnnotation> getResult() {
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        canceled = true;
    }

}
