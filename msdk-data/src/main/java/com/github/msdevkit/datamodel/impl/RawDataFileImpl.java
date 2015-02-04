/* 
 * Copyright 2015 MSDK Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.github.msdevkit.datamodel.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.msdevkit.datamodel.MsScan;
import com.github.msdevkit.datamodel.RawDataFile;
import com.google.common.collect.Range;

/**
 * RawDataFile implementation.
 */
class RawDataFileImpl extends DataPointStoreImpl implements RawDataFile {

    private @Nonnull String rawDataFileName;
    private @Nonnull RawDataFile originalRawDataFile;
    private final List<MsScan> scans;

    RawDataFileImpl() {
	rawDataFileName = "New file";
	originalRawDataFile = this;
	scans = new ArrayList<MsScan>();
    }

    @Override
    public String getName() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void setName(String name) {
	// TODO Auto-generated method stub

    }

    @Override
    public void addScan(MsScan scan) {
	// TODO Auto-generated method stub

    }

    @Override
    public void removeScan(MsScan scan) {
	// TODO Auto-generated method stub

    }

    @Override
    public List<MsScan> getScans() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<MsScan> getScans(Integer msLevel, Range<Double> rtRange) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public List<Integer> getMSLevels() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public MsScan getScan(int scanNumber) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Range<Double> getRawDataMZRange() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Range<Double> getRawDataScanRange() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Range<Double> getRawDataRTRange() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Range<Double> getRawDataMZRange(Integer msLevel) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Range<Double> getRawDataScanRange(Integer msLevel) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public Range<Double> getRawDataRTRange(Integer msLevel) {
	// TODO Auto-generated method stub
	return null;
    }

}