/* 
 * (C) Copyright 2015-2016 by MSDK Development Team
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

package io.github.msdk.id.localdatabasesearch;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Range;

import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.featuretables.ColumnName;
import io.github.msdk.datamodel.featuretables.FeatureTable;
import io.github.msdk.datamodel.featuretables.FeatureTableColumn;
import io.github.msdk.datamodel.featuretables.FeatureTableRow;
import io.github.msdk.datamodel.impl.MSDKObjectBuilder;
import io.github.msdk.datamodel.ionannotations.IonAnnotation;
import io.github.msdk.datamodel.rawdata.ChromatographyInfo;
import io.github.msdk.util.RTTolerance;
import io.github.msdk.util.tolerances.MzTolerance;

/**
 * This class searches through a feature table to find hits in a local database
 * using m/z and retention time values.
 */
public class LocalDatabaseSearchMethod implements MSDKMethod<Void> {

    private final @Nonnull FeatureTable featureTable;
    private final @Nonnull List<IonAnnotation> ionAnnotations;
    private final @Nonnull MzTolerance mzTolerance;
    private final @Nonnull RTTolerance rtTolerance;

    private boolean canceled = false;
    private int processedFeatures = 0, totalFeatures = 0;

    /**
     * <p>
     * Constructor for LocalDatabaseSearchMethod.
     * </p>
     *
     * @param featureTable
     *            a {@link io.github.msdk.datamodel.featuretables.FeatureTable}
     *            object.
     * @param ionAnnotations
     *            a {@link java.util.List} of
     *            {@link io.github.msdk.datamodel.ionannotations.IonAnnotation}
     *            objects.
     * @param mzTolerance
     *            an object that implements the {@link io.github.msdk.util.tolerances.MZTolerance} interface.
     * @param rtTolerance
     *            a {@link io.github.msdk.util.RTTolerance} object.
     */
    public LocalDatabaseSearchMethod(@Nonnull FeatureTable featureTable,
            @Nonnull List<IonAnnotation> ionAnnotations,
            @Nonnull MzTolerance mzTolerance,
            @Nonnull RTTolerance rtTolerance) {
        this.featureTable = featureTable;
        this.ionAnnotations = ionAnnotations;
        this.mzTolerance = mzTolerance;
        this.rtTolerance = rtTolerance;
    }

    /** {@inheritDoc} */
    @Override
    public Void execute() throws MSDKException {

        totalFeatures = featureTable.getRows().size();
        FeatureTableColumn<List<IonAnnotation>> ionAnnotationColumn = featureTable
                .getColumn(ColumnName.IONANNOTATION, null);

        // Create ion annotation column if it is not present in the table
        if (ionAnnotationColumn == null) {
            ionAnnotationColumn = MSDKObjectBuilder
                    .getIonAnnotationFeatureTableColumn();
            featureTable.addColumn(ionAnnotationColumn);
        }

        // Loop through all features in the feature table
        for (FeatureTableRow row : featureTable.getRows()) {

            final Double mz = row.getMz();
            final ChromatographyInfo rt = row.getChromatographyInfo();
            if ((mz == null) || (rt == null))
                continue;

            // Row values
            Range<Double> mzRange = mzTolerance.getToleranceRange(mz);
            Range<Double> rtRange = rtTolerance
                    .getToleranceRange(rt.getRetentionTime());
            List<IonAnnotation> rowIonAnnotations = row
                    .getData(ionAnnotationColumn);

            // Empty rowIonAnnotations
            if (rowIonAnnotations == null)
                rowIonAnnotations = new ArrayList<IonAnnotation>();

            // Loop through all ion annotations from the local database
            for (IonAnnotation ionAnnotation : ionAnnotations) {

                // Ion values
                final Double ionMz = ionAnnotation.getExpectedMz();
                final ChromatographyInfo ionChromInfo = ionAnnotation
                        .getChromatographyInfo();
                if ((ionMz == null) || (ionChromInfo == null))
                    continue;

                // Convert from seconds to minutes
                double ionRt = ionChromInfo.getRetentionTime() / 60.0;
                final boolean mzMatch = mzRange.contains(ionMz);
                final boolean rtMatch = rtRange.contains(ionRt);

                // If match, add the ion annotation to the list
                if (mzMatch && rtMatch) {

                    // If first ion annotation is empty then remove it
                    if (rowIonAnnotations.size() > 0) {
                        IonAnnotation firstionAnnotation = rowIonAnnotations
                                .get(0);
                        if (firstionAnnotation.isNA())
                            rowIonAnnotations.remove(0);
                    }

                    // Only add annotation if it is not already present
                    boolean addIon = true;
                    for (IonAnnotation ionAnnotations : rowIonAnnotations) {
                        if (ionAnnotations.compareTo(ionAnnotation) == 0)
                            addIon = false;
                    }
                    if (addIon)
                        rowIonAnnotations.add(ionAnnotation);
                }

            }

            // Update the ion annotations of the feature
            row.setData(ionAnnotationColumn, rowIonAnnotations);

            if (canceled)
                return null;

            processedFeatures++;
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public Float getFinishedPercentage() {
        return totalFeatures == 0 ? null
                : (float) processedFeatures / totalFeatures;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public Void getResult() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        canceled = true;
    }

}
