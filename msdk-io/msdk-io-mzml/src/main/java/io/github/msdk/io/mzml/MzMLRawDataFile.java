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

package io.github.msdk.io.mzml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import io.github.msdk.datamodel.chromatograms.Chromatogram;
import io.github.msdk.datamodel.files.FileType;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

class MzMLRawDataFile implements RawDataFile {

  private static final @Nonnull FileType fileType = FileType.MZML;

  private final @Nonnull File sourceFile;
  private @Nullable MzMLUnmarshaller parser;

  private final @Nonnull List<MsScan> msScans;
  private final @Nonnull List<Chromatogram> chromatograms;

  private @Nonnull String name;

  /**
   * <p>
   * Constructor for MzMLRawDataFile.
   * </p>
   *
   * @param sourceFile a {@link java.io.File} object.
   * @param parser a {@link uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller} object.
   * @param msFunctions a {@link java.util.List} object.
   * @param msScans a {@link java.util.List} object.
   * @param chromatograms a {@link java.util.List} object.
   */
  public MzMLRawDataFile(@Nonnull File sourceFile, @Nonnull MzMLUnmarshaller parser,
      List<MsScan> msScans, List<Chromatogram> chromatograms) {
    this.sourceFile = sourceFile;
    this.parser = parser;
    this.name = sourceFile.getName();
    this.msScans = msScans;
    this.chromatograms = chromatograms;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public String getName() {
    return name;
  }


  /** {@inheritDoc} */
  @Override
  @Nullable
  public Optional<File> getOriginalFile() {
    return Optional.ofNullable(sourceFile);
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public FileType getRawDataFileType() {
    return fileType;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public List<String> getMsFunctions() {
    ArrayList<String> msFunctionList = new ArrayList<>();
    for (MsScan scan : msScans) {
      String f = scan.getMsFunction();
      if ((f != null) && (!msFunctionList.contains(f)))
        msFunctionList.add(f);
    }
    return msFunctionList;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public List<MsScan> getScans() {
    return ImmutableList.copyOf(msScans);
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public List<Chromatogram> getChromatograms() {
    return ImmutableList.copyOf(chromatograms);
  }

  /** {@inheritDoc} */
  @Override
  public void dispose() {
    parser = null;
  }

  @Nullable
  MzMLUnmarshaller getParser() {
    return parser;
  }



}
