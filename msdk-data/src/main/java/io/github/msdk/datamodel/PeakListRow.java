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

package io.github.msdk.datamodel;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * 
 */
public interface PeakListRow {

    /**
     * @return
     */
    @Nonnull
    PeakList getParentPeakList();

    /**
     * Returns ID of this row
     */
    int getId();

    /**
     * Returns number of peaks assigned to this row
     */
    int getNumberOfColumns();

    /**
     * Return peaks assigned to this row
     */
    <DataType> List<PeakListColumn<DataType>> getColumns();

    /**
     * 
     */
    <DataType> Optional<DataType> getData(PeakListColumn<DataType> column);

}
