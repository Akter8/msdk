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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.lang.Math;

import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.RawDataFile;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;


/**
 * <p>
 * SliceSparseMatrix class is used for slicing the sparse matrix of raw data as per given mz value.
 * slice contains intensities for one mz value for different scans.
 * </p>
 */
public class SliceSparseMatrix {
	
	/**
	 * <p>
	 *  tripletMap is used for creating MultiKeyMap type of hashmap from raw data file. 
	 * </p>
	 */
	private final MultiKeyMap tripletMap;
	
	/**
	 * <p>
	 *  filterListOfTriplet is used for adding intensities for same mz values under same scan numbers. 
	 * </p>
	 */
	private final List<SparseMatrixTriplet> filterListOfTriplet;
	
	/**
	 * <p>
	 *  maxIntensityIndex is used for keeping track of next maximum intensity in the loop. 
	 * </p>
	 */
	private int maxIntensityIndex=0;
	
	/**
	 * <p>
	 *  roundMz is used for rounding mz value. 
	 * </p>
	 */
	private final int roundMz = 100;
	
	/**
	 * <p>
	 *  listOfScans is used for getting scan objects from raw data file. 
	 * </p>
	 */
	private final List<MsScan> listOfScans;
	
	/**
	 * <p>
	 * This is the data model for creating triplet representation of sparse matrix. 
	 * </p>
	 */
	public static class SparseMatrixTriplet {
		public int mz;
		public int scanNumber;
		public float intensity;
		public float rt;
		public boolean removed;
	}
	
	/**
	   * <p>
	   * This constructor takes raw data file and create the triplet map which contains information
	   * such as mz,intensity,rt,scan number
	   * </p>
	   * 
	   *  @param rawFile a {@link io.github.msdk.datamodel.rawdata.RawDataFile} object
	   */
	public SliceSparseMatrix(RawDataFile rawFile){
		listOfScans = rawFile.getScans();
		List<SparseMatrixTriplet> listOfTriplet = new ArrayList<SparseMatrixTriplet>();
	    
	    for(int i=0;i<listOfScans.size();i++){
	    	MsScan scan = listOfScans.get(i);
	    	
	    	if(scan==null)
	    		continue;
	    	
	    	double mzBuffer[];
	    	float intensityBuffer[];
	    	Float rt;
	    	mzBuffer = scan.getMzValues();
	    	intensityBuffer = scan.getIntensityValues();
	    	rt = scan.getRetentionTime();
	    	
	    	if(rt==null)
	    		continue;
	    	
	    	for(int j=0;j<mzBuffer.length;j++){
	    		SparseMatrixTriplet triplet = new SparseMatrixTriplet();
	    		triplet.intensity = intensityBuffer[j];
	    		triplet.mz = roundMZ(mzBuffer[j]);
	    		triplet.scanNumber  = i;
	    		triplet.rt = rt;
	    		triplet.removed = false;
	    		listOfTriplet.add(triplet);
	    	}
	    }
	    
	   	    
	    
	    Comparator<SparseMatrixTriplet> compare = new Comparator<SparseMatrixTriplet>() {
			
			@Override
			public int compare(SparseMatrixTriplet o1, SparseMatrixTriplet o2) {
				
				Integer  scan1 = o1.scanNumber;
				Integer  scan2 = o2.scanNumber;
				int scanCompare = scan1.compareTo(scan2);
				
				if(scanCompare!=0){
					return scanCompare;
				}
				else {
					Integer  mz1 = o1.mz;
					Integer  mz2 = o2.mz;
					return mz1.compareTo(mz2);
				}
			}
		};
		
		
		Collections.sort(listOfTriplet, compare);	
		
		filterListOfTriplet = new ArrayList<SparseMatrixTriplet>();
		SparseMatrixTriplet currTriplet = new SparseMatrixTriplet();
		SparseMatrixTriplet lastFilterTriplet = new SparseMatrixTriplet();
		tripletMap = new MultiKeyMap ();
		int index = 0;
		filterListOfTriplet.add(listOfTriplet.get(0));
		for(int i=1;i<listOfTriplet.size();i++){
			currTriplet = listOfTriplet.get(i);
			lastFilterTriplet = filterListOfTriplet.get(index);
			if(currTriplet.mz == lastFilterTriplet.mz && currTriplet.scanNumber == lastFilterTriplet.scanNumber){
				lastFilterTriplet.intensity += currTriplet.intensity;
			}
			else{
				filterListOfTriplet.add(currTriplet);
				tripletMap.put(currTriplet.scanNumber, currTriplet.mz,currTriplet);
				index++;
			}
			
		}
		
	}
		
	 /**
	   * <p>
	   * This method returns the MultiKeyMap slice of data for given mz,lowerScanBound,upperScanBound
	   * </p>
	   * 
	   * @param mz a {@link java.lang.Double} object
	   * @param lowerScanBound a {@link java.lang.Integer} object
	   * @param upperScanBound a {@link java.lang.Integer} object
	   * @return sliceMap a {@link org.apache.commons.collections4.map.MultiKeyMap} object
	   */
	public MultiKeyMap getSlice(double mz,int lowerScanBound,int upperScanBound){
		
		int roundedmz = roundMZ(mz);
		MultiKeyMap  sliceMap = new MultiKeyMap ();
				
		for(int i = lowerScanBound;i<=upperScanBound;i++){
			if(tripletMap.containsKey(new Integer(i),new Integer(roundedmz))){
				SparseMatrixTriplet triplet = (SparseMatrixTriplet)tripletMap.get(new Integer(i),new Integer(roundedmz));
				sliceMap.put(i, roundedmz,triplet);
			}
			else{
				sliceMap.put(i, roundedmz, null);
			}
		}
			
		return sliceMap;
	}
	
	/**
	   * <p>
	   * This method finds next maximum intensity from filterListOfTriplet
	   * </p>
	   * 
	   * @return tripletObject a {@link io.github.msdk.featdet.ADAP3D.common.algorithms.SliceSparseMatrix.SparseMatrixTriplet} object 
	   */
	public SparseMatrixTriplet findNextMaxIntensity(){
		
		SparseMatrixTriplet tripletObject = null;
		 Comparator<SparseMatrixTriplet> compare = new Comparator<SparseMatrixTriplet>() {
				
				@Override
				public int compare(SparseMatrixTriplet o1, SparseMatrixTriplet o2) {
					
					Float  intensity1 = o1.intensity;
					Float  intensity2 = o2.intensity;
					int intensityCompare = intensity2.compareTo(intensity1);
					return intensityCompare;
				}
			};
			Collections.sort(filterListOfTriplet,compare);
			
			for(int i=maxIntensityIndex;i<filterListOfTriplet.size();i++){
				if(filterListOfTriplet.get(i).removed == false){
					tripletObject = filterListOfTriplet.get(i);
					maxIntensityIndex=i+1;
					break;
				}
				
			}
			return tripletObject;
	}
	
	/**
	   * <p>
	   * This method returns sorted list of ContinuousWaveletTransform.DataPoint object.Object contain retention time and intensity values 
	   * </p>
	   * 
	   * @param slice a {@link org.apache.commons.collections4.map.MultiKeyMap} object
	   * @return listOfDataPoint a {@link io.github.msdk.featdet.ADAP3D.common.algorithms.SliceSparseMatrix.SparseMatrixTriplet} list
	   */
	public List<ContinuousWaveletTransform.DataPoint> getCWTDataPoint(MultiKeyMap slice){
		
		MapIterator iterator = slice.mapIterator();
		List<ContinuousWaveletTransform.DataPoint> listOfDataPoint = new ArrayList<ContinuousWaveletTransform.DataPoint>();
		
		while (iterator.hasNext())  {
			ContinuousWaveletTransform.DataPoint dataPoint = new ContinuousWaveletTransform.DataPoint();
			iterator.next();
			MultiKey sliceKey = (MultiKey) iterator.getKey();
			 SparseMatrixTriplet triplet = (SparseMatrixTriplet)slice.get(sliceKey);
			 if(triplet != null){
				 dataPoint.rt = triplet.rt/60;
				 dataPoint.intensity = triplet.intensity;
				 listOfDataPoint.add(dataPoint);
				}
			 else{
				 MsScan scan = listOfScans.get((int) sliceKey.getKey(0));
				 dataPoint.rt = scan.getRetentionTime()/60;
				 dataPoint.intensity = 0.0;
				 listOfDataPoint.add(dataPoint);
			 }
		  }
		Comparator<ContinuousWaveletTransform.DataPoint> compare = new Comparator<ContinuousWaveletTransform.DataPoint>() {
			
			@Override
			public int compare(ContinuousWaveletTransform.DataPoint o1, ContinuousWaveletTransform.DataPoint o2) {
				Double rt1 = o1.rt;
				Double rt2 = o2.rt;
				return rt1.compareTo(rt2);
			}
		};
		
		Collections.sort(listOfDataPoint,compare);
		
		return listOfDataPoint;
	}

	/**
	   * <p>
	   * This method removes data points from whole data set for given mz,lowerscanbound and upperscanbound 
	   * </p>
	   * 
	   * @param mz a {@link java.lang.Double} object
	   * @param lowerScanBound a {@link java.lang.Integer} object
	   * @param upperScanBound a {@link java.lang.Integer} object
	   * @return tripletMap a {@link org.apache.commons.collections4.map.MultiKeyMap} object
	   */
	public MultiKeyMap removeDataPoints(double mz,int lowerScanBound,int upperScanBound){
		int roundedmz = roundMZ(mz);
		for(int i = lowerScanBound;i<=upperScanBound;i++){
			if(tripletMap.containsKey(new Integer(i),new Integer(roundedmz))){
				SparseMatrixTriplet triplet = (SparseMatrixTriplet)tripletMap.get(new Integer(i),new Integer(roundedmz));
				triplet.removed = true;
			}
		}
		return tripletMap;
	}
	
	/**
	   * <p>
	   * This method rounds mz value based on roundMz variable  
	   * </p>
	   * 
	   * @param mz a {@link java.lang.Double} object
	   * @return roundedmz a {@link java.lang.Integer} object
	   */
	private int roundMZ(double mz){
		int roundedmz = (int)Math.round(mz*roundMz);
		return roundedmz;
	}

	/**
	   * <p>
	   * This method sets maxIntensityIndex to 0
	   * </p>
	   */
	public void setMaxIntensityIndexZero(){
		maxIntensityIndex = 0;
	}

}
