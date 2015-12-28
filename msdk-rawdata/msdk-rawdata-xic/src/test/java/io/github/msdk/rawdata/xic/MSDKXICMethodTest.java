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

package io.github.msdk.rawdata.xic;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Range;

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.chromatograms.Chromatogram;
import io.github.msdk.datamodel.chromatograms.ChromatogramType;
import io.github.msdk.datamodel.datastore.DataPointStore;
import io.github.msdk.datamodel.datastore.DataPointStoreFactory;
import io.github.msdk.datamodel.rawdata.ChromatographyInfo;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.io.mzml.MzMLFileImportMethod;

public class MSDKXICMethodTest {

    private static final String TEST_DATA_PATH = "src/test/resources/";

    @Test
    public void testXIC() throws MSDKException {

        // Import the file
        File inputFile = new File(TEST_DATA_PATH + "orbitrap_300-600mz.mzML");
        Assert.assertTrue("Cannot read test data", inputFile.canRead());
        MzMLFileImportMethod importer = new MzMLFileImportMethod(inputFile);
        RawDataFile rawFile = importer.execute();
        Assert.assertNotNull(rawFile);
        Assert.assertEquals(1.0, importer.getFinishedPercentage(), 0.0001);

        // Input parameters
        List<MsScan> scans = rawFile.getScans();
        Range<Double> mzRange = Range.closed(400.0, 500.0);
        ChromatogramType chromatogramType = ChromatogramType.XIC;
        DataPointStore store = DataPointStoreFactory.getMemoryDataStore();

        // Execute the method
        MSDKXICMethod xicMethod = new MSDKXICMethod(rawFile, scans, mzRange,
                chromatogramType, store);
        Chromatogram newChromatogram = xicMethod.execute();

        // The result of the method can't be null
        Assert.assertNotNull(newChromatogram);
        Assert.assertEquals(1.0f, xicMethod.getFinishedPercentage(), 0.0001f);

        // The chromatogram should have one data point for each scan
        Assert.assertEquals(new Integer(scans.size()),
                newChromatogram.getNumberOfDataPoints());

        // Check if the resulting chromatogram matches the requested parameters
        double mzValues[] = newChromatogram.getMzValues();
        float intensityValues[] = newChromatogram.getIntensityValues();
        ChromatographyInfo rtValues[] = newChromatogram.getRetentionTimes();

        for (int i = 0; i < newChromatogram.getNumberOfDataPoints(); i++) {

            Assert.assertNotNull(rtValues[i]);

            Assert.assertEquals(
                    rawFile.getScans().get(i).getChromatographyInfo(),
                    rtValues[i]);

            Assert.assertTrue(mzRange.contains(mzValues[i]));

            Assert.assertTrue(intensityValues[i] >= 0f);
        }

    }
}
