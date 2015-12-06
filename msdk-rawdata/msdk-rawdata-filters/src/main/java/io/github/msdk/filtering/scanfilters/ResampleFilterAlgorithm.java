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
package io.github.msdk.filtering.scanfilters;

import com.google.common.collect.Range;
import io.github.msdk.datamodel.datapointstore.DataPointStore;
import io.github.msdk.datamodel.impl.MSDKObjectBuilder;
import io.github.msdk.datamodel.msspectra.MsSpectrumDataPointList;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.filtering.MSDKFilteringAlgorithm;
import javax.annotation.Nonnull;

public class ResampleFilterAlgorithm implements MSDKFilteringAlgorithm {

    private final @Nonnull double binSize;
    private final @Nonnull DataPointStore store;
    
    public ResampleFilterAlgorithm(@Nonnull double binSize, @Nonnull DataPointStore store) {
        this.binSize = binSize;
        this.store = store;
    }

    @Override
    public MsScan performFilter(@Nonnull MsScan scan) {
        Range<Double> mzRange = scan.getMzRange();
	int numberOfBins = (int) Math.round((mzRange.upperEndpoint() - mzRange
		.lowerEndpoint()) / binSize);
	if (numberOfBins <= 0) {
	    numberOfBins++;
	}

        // Create data point list object and fill it with the scan data points
        MsSpectrumDataPointList dataPoints = MSDKObjectBuilder.getMsSpectrumDataPointList();
        scan.getDataPoints(dataPoints);
	float intensityValues[] = dataPoints.getIntensityBuffer();        
        
        // Create the array with the intensity values for each bin
        Float[] newY = new Float[numberOfBins];
        int intVal = 0;
        for(int i = 0; i < numberOfBins; i++){
            newY[i] = 0.0f;
            for(int j = 0; j < binSize; j++){
                if(intVal < intensityValues.length){
                    newY[i] += intensityValues[intVal++];
                }
            }
            newY[i] /= (float)binSize;
        }

	// Set the new m/z value in the middle of the bin
	double newX = mzRange.lowerEndpoint() + binSize / 2.0;
        
	// Creates new DataPoints
        dataPoints.clear();
        for (Float newIntensity : newY) {
            dataPoints.add(newX, newIntensity);
            newX += binSize;
        }
        
        // Return a new scan with the new data points
        MsScan result = MSDKObjectBuilder.getMsScan(store, scan.getScanNumber(), scan.getMsFunction());
        result.setDataPoints(dataPoints);
        result.setChromatographyInfo(scan.getChromatographyInfo());
        result.setRawDataFile(scan.getRawDataFile());

        return result;
    }

    @Override
    public String getName() {
        return "Resample filter";
    }

}
