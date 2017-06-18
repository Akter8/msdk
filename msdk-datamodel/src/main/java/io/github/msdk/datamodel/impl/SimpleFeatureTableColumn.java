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

package io.github.msdk.datamodel.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.github.msdk.datamodel.featuretables.FeatureTableColumn;
import io.github.msdk.datamodel.featuretables.Sample;

/**
 * Implementation of FeatureTableColumn
 *
 * @author plusik
 * @version $Id: $Id
 */
public class SimpleFeatureTableColumn<DataType> implements FeatureTableColumn<DataType> {

  private @Nonnull String name;
  private @Nonnull Class<DataType> dataTypeClass;
  private @Nullable Sample simpleSample;

  /**
   * <p>Constructor for SimpleFeatureTableColumn.</p>
   *
   * @param name a {@link java.lang.String} object.
   * @param dataTypeClass a {@link java.lang.Class} object.
   * @param sample a {@link io.github.msdk.datamodel.featuretables.Sample} object.
   */
  public SimpleFeatureTableColumn(@Nonnull String name, @Nonnull Class<DataType> dataTypeClass,
      @Nullable Sample sample) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(dataTypeClass);
    this.name = name;
    this.dataTypeClass = dataTypeClass;
    this.simpleSample = sample;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull String getName() {
    return name;
  }

  /** {@inheritDoc} */
  @Override
  public void setName(@Nonnull String name) {
    Preconditions.checkNotNull(name);
    this.name = name;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull Class<DataType> getDataTypeClass() {
    return dataTypeClass;
  }

  /** {@inheritDoc} */
  @Override
  public Sample getSample() {
    return simpleSample;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return String.format("%s (%s)", name, dataTypeClass.getSimpleName());
  }

}
