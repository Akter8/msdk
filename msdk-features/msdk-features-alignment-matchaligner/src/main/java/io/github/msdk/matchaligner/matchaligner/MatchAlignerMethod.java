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

package io.github.msdk.matchaligner.matchaligner;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Range;

import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.datapointstore.DataPointStore;
import io.github.msdk.datamodel.featuretables.ColumnName;
import io.github.msdk.datamodel.featuretables.FeatureTable;
import io.github.msdk.datamodel.featuretables.FeatureTableColumn;
import io.github.msdk.datamodel.featuretables.FeatureTableRow;
import io.github.msdk.datamodel.featuretables.Sample;
import io.github.msdk.datamodel.impl.MSDKObjectBuilder;
import io.github.msdk.datamodel.ionannotations.IonAnnotation;
import io.github.msdk.util.FeatureTableUtil;
import io.github.msdk.util.MZTolerance;
import io.github.msdk.util.RTTolerance;

/**
 * This class aligns feature tables based on a match score. The score is
 * calculated based on the mass and retention time of each peak using a set of
 * tolerances.
 */
public class MatchAlignerMethod implements MSDKMethod<FeatureTable> {

    // Variables
    private final @Nonnull MZTolerance mzTolerance;
    private final @Nonnull RTTolerance rtTolerance;
    private final @Nonnull int mzWeight;
    private final @Nonnull int rtWeight;
    private final @Nonnull boolean requireSameCharge;
    private final @Nonnull boolean requireSameAnnotation;
    private final @Nonnull String featureTableName;
    private final @Nonnull DataPointStore dataStore;
    private final @Nonnull List<FeatureTable> featureTables;
    private final @Nonnull FeatureTable result;
    private boolean canceled = false;
    private int processedFeatures = 0, totalFeatures = 0;

    // ID counter for the new feature table
    private int newRowID = 1;

    /**
     * <p>
     * Constructor for MatchAlignerMethod.
     * </p>
     *
     * @param featureTables
     *            a {@link java.lang.List} of
     *            {@link io.github.msdk.datamodel.featuretables.FeatureTable}
     *            objects.
     * @param dataStore
     *            a
     *            {@link io.github.msdk.datamodel.datapointstore.DataPointStore}
     *            object.
     * @param mzTolerance
     *            a {@link io.github.msdk.util.MZTolerance} object.
     * @param RTTolerance
     *            a {@link io.github.msdk.util.RTTolerance} object.
     * @param mzWeight
     *            a {@link java.lang.Integer} object.
     * @param rtWeight
     *            a {@link java.lang.Integer} object.
     * @param requireSameCharge
     *            a {@link java.lang.Boolean} object.
     * @param requireSameAnnotation
     *            a {@link java.lang.Boolean} object.
     * @param featureTableName
     *            a {@link java.lang.String} object.
     */
    public MatchAlignerMethod(@Nonnull List<FeatureTable> featureTables,
            @Nonnull DataPointStore dataStore, @Nonnull MZTolerance mzTolerance,
            @Nonnull RTTolerance rtTolerance, @Nonnull int mzWeight,
            @Nonnull int rtWeight, @Nonnull boolean requireSameCharge,
            @Nonnull boolean requireSameAnnotation,
            @Nonnull String featureTableName) {
        this.featureTables = featureTables;
        this.dataStore = dataStore;
        this.mzTolerance = mzTolerance;
        this.rtTolerance = rtTolerance;
        this.mzWeight = mzWeight;
        this.rtWeight = rtWeight;
        this.requireSameCharge = requireSameCharge;
        this.requireSameAnnotation = requireSameAnnotation;
        this.featureTableName = featureTableName;

        // Make a new feature table
        result = MSDKObjectBuilder.getFeatureTable(featureTableName, dataStore);
    }

    /** {@inheritDoc} */
    @Override
    public FeatureTable execute() throws MSDKException {

        // Calculate number of feature to process. Each feature will be
        // processed twice: first for score calculation and then for actual
        // alignment.
        for (FeatureTable featureTable : featureTables) {
            totalFeatures += featureTable.getRows().size() * 2;
        }

        // Iterate through all feature tables
        Boolean firstFeatureTable = true;
        for (FeatureTable featureTable : featureTables) {

            // Add columns from the original feature table to the result table
            for (FeatureTableColumn<?> column : featureTable.getColumns()) {
                if (firstFeatureTable)
                    result.addColumn(column);
                else if (column.getSample() != null)
                    result.addColumn(column);
            }
            firstFeatureTable = false;

            // Create a sorted array of matching scores between two rows
            List<RowVsRowScore> scoreSet = new ArrayList<RowVsRowScore>();

            // Calculate scores for all possible alignments of this row
            for (FeatureTableRow row : featureTable.getRows()) {

                // Calculate range limits for the current row
                Range<Double> mzRange = mzTolerance
                        .getToleranceRange(row.getMz());
                Range<Double> rtRange = rtTolerance.getToleranceRange(
                        row.getChromatographyInfo().getRetentionTime());

                // Get all rows of the aligned feature table within the m/z and
                // RT limits
                List<FeatureTableRow> candidateRows = result
                        .getRowsInsideRange(rtRange, mzRange);

                // Calculate scores and store them
                for (FeatureTableRow candidateRow : candidateRows) {

                    // Check charge
                    if (requireSameCharge) {
                        FeatureTableColumn<Integer> chargeColumn1 = featureTable
                                .getColumn(ColumnName.CHARGE, null);
                        FeatureTableColumn<Integer> chargeColumn2 = result
                                .getColumn(ColumnName.CHARGE, null);
                        if (!row.getData(chargeColumn1)
                                .equals(candidateRow.getData(chargeColumn2)))
                            continue;
                    }

                    // Check ion annotation
                    if (requireSameAnnotation) {
                        FeatureTableColumn<IonAnnotation> ionAnnotationColumn1 = featureTable
                                .getColumn("Ion Annotation", null,
                                        IonAnnotation.class);
                        FeatureTableColumn<IonAnnotation> ionAnnotationColumn2 = result
                                .getColumn("Ion Annotation", null,
                                        IonAnnotation.class);
                        if (row.getData(ionAnnotationColumn1)
                                .compareTo(candidateRow
                                        .getData(ionAnnotationColumn2)) != 0)
                            continue;
                    }

                    // Calculate score
                    double mzLength = mzRange.upperEndpoint()
                            - mzRange.lowerEndpoint();
                    double rtLength = rtRange.upperEndpoint()
                            - rtRange.lowerEndpoint();
                    RowVsRowScore score = new RowVsRowScore(row, candidateRow,
                            mzLength / 2.0, mzWeight, rtLength / 2.0, rtWeight);

                    // Add the score to the array
                    scoreSet.add(score);

                }

                processedFeatures++;

                if (canceled)
                    return null;
            }

            // Create a table of mappings for best scores
            Hashtable<FeatureTableRow, FeatureTableRow> alignmentMapping = new Hashtable<FeatureTableRow, FeatureTableRow>();

            // Iterate scores by descending order
            Iterator<RowVsRowScore> scoreIterator = scoreSet.iterator();
            while (scoreIterator.hasNext()) {
                RowVsRowScore score = scoreIterator.next();

                // Check if the row is already mapped
                if (alignmentMapping.containsKey(score.getFeatureTableRow()))
                    continue;

                // Check if the aligned row is already filled
                if (alignmentMapping.containsValue(score.getAlignedRow()))
                    continue;

                alignmentMapping.put(score.getFeatureTableRow(),
                        score.getAlignedRow());
            }

            // Align all rows using the mapping
            for (FeatureTableRow sourceRow : featureTable.getRows()) {
                FeatureTableRow targetRow = alignmentMapping.get(sourceRow);

                // If we have no mapping for this row, add a new one
                if (targetRow == null) {
                    targetRow = MSDKObjectBuilder.getFeatureTableRow(result,
                            newRowID);
                    result.addRow(targetRow);
                    FeatureTableColumn<Integer> column = result
                            .getColumn(ColumnName.ID, null);
                    targetRow.setData(column, newRowID);
                    newRowID++;
                }

                // Add all features from the original row to the aligned row
                for (Sample sample : sourceRow.getFeatureTable().getSamples()) {
                    FeatureTableUtil.copyFeatureValues(sourceRow, targetRow,
                            sample);
                }

                // Combine common values from the original row with the aligned row
                FeatureTableUtil.copyCommonValues(sourceRow, targetRow, true);

                processedFeatures++;
            }

            // Re-calculate average row averages
            FeatureTableUtil.recalculateAverages(result);

            if (canceled)
                return null;

        }

        // Return the new feature table
        return result;
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
    public FeatureTable getResult() {
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        canceled = true;
    }

}
