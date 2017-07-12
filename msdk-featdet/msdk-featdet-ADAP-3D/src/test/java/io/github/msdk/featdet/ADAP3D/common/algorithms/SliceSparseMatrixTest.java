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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.featdet.ADAP3D.common.algorithms.SliceSparseMatrix.Triplet;
import io.github.msdk.io.mzml.MzMLFileImportMethod;

import org.apache.commons.collections4.map.MultiKeyMap;



public class SliceSparseMatrixTest {

  private static RawDataFile rawFile;
  private static SliceSparseMatrix objSliceSparseMatrix;

  private static Path getResourcePath(String resource) throws MSDKException {
    final URL url = SliceSparseMatrixTest.class.getClassLoader().getResource(resource);
    try {
      return Paths.get(url.toURI()).toAbsolutePath();
    } catch (URISyntaxException e) {
      throw new MSDKException(e);
    }
  }

  @BeforeClass
  public static void loadData() throws MSDKException {

    // Import the file
    String file = "orbitrap_300-600mz.mzML";
    Path path = getResourcePath(file);
    File inputFile = path.toFile();
    Assert.assertTrue("Cannot read test data", inputFile.canRead());
    MzMLFileImportMethod importer = new MzMLFileImportMethod(inputFile);
    rawFile = importer.execute();
    objSliceSparseMatrix = new SliceSparseMatrix(rawFile);
    Assert.assertNotNull(rawFile);
    Assert.assertEquals(1.0, importer.getFinishedPercentage(), 0.0001);
  }


  @Test
  public void getHorizontalSlice() throws MSDKException, IOException {
    MultiKeyMap<Integer, Triplet> slice =
        objSliceSparseMatrix.getHorizontalSlice(301.15106201171875, 0, 208);
    int size = slice.size();
    for (int i = 0; i < size; i++) {
      Assert.assertTrue(slice.containsKey(new Integer(i), new Integer(3011511)));
    }
    Assert.assertEquals(209, size);
  }

  @Test
  public void getVerticalSlice() throws MSDKException, IOException {
    List<SliceSparseMatrix.VerticalSliceDataPoint> slice = objSliceSparseMatrix.getVerticalSlice(5);
    Assert.assertEquals(46004, slice.size());
  }

  @Test
  public void testFindNextMaxIntensity() throws MSDKException, IOException {
    double intensityValues[] = {8538462.0, 8521695.0, 8365356.0};
    for (int i = 0; i < 3; i++) {
      Assert.assertEquals(intensityValues[i], objSliceSparseMatrix.findNextMaxIntensity().intensity,
          0);
    }
  }

  @Test
  public void testGetRetentionTimeGetIntensity() throws MSDKException, IOException {
    MultiKeyMap<Integer, Triplet> slice =
        objSliceSparseMatrix.getHorizontalSlice(301.15106201171875, 0, 208);
    List<ContinuousWaveletTransform.DataPoint> listOfDataPoint =
        objSliceSparseMatrix.getCWTDataPoint(slice);
    Assert.assertNotNull(listOfDataPoint);
  }

  @Test
  public void testRemoveDataPoints() throws MSDKException, IOException {
    MultiKeyMap<Integer, Triplet> updatedTripletMap =
        objSliceSparseMatrix.removeDataPoints(301.15106201171875, 0, 10);
    for (int i = 0; i < 11; i++) {
      SliceSparseMatrix.Triplet triplet =
          (SliceSparseMatrix.Triplet) updatedTripletMap.get(new Integer(i), new Integer(3011511));
      if (triplet != null)
        Assert.assertTrue(triplet.removed);
    }
  }
}
