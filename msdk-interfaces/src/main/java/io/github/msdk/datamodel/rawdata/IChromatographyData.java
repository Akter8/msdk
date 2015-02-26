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

package io.github.msdk.datamodel.rawdata;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * This class represents the chromatography information of an MS scan or a
 * detected feature (peak). For convenience, this interface is immutable, so it
 * can be passed by reference and safely used by multiple threads. This
 * interface also extends Comparable, so we can use the Range class to define
 * ranges of retention times etc. The comparator method should compare two
 * instances by retention time, secondary retention time, and drift time in this
 * order.
 */
@Immutable
public interface IChromatographyData extends Comparable<IChromatographyData> {

    /**
     * Returns retention time in minutes, or null if no retention time is
     * defined.
     * 
     * @return Retention time in minutes, or null.
     */
    @Nullable
    Double getRetentionTime();

    /**
     * Returns secondary retention time in minutes (for two-dimensional
     * separations such as GCxGC-MS), or null if no secondary retention time is
     * defined.
     * 
     * @return Secondary retention time in minutes, or null.
     */
    @Nullable
    Double getSecondaryRetentionTime();

    /**
     * Returns ion drift time in ms (for ion mobility experiments), or null if
     * no drift time is defined.
     * 
     * @return Drift time in ms, or null.
     */
    @Nullable
    Double getIonDriftTime();

}
