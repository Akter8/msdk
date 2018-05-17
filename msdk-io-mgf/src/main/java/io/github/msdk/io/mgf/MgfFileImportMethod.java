/*
 * (C) Copyright 2015-2018 by MSDK Development Team
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

package io.github.msdk.io.mgf;

import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.MsSpectrumType;
import io.github.msdk.util.ArrayUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MgfFileImportMethod implements MSDKMethod<Collection<MgfMsSpectrum>> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private Collection<MgfMsSpectrum> spectra;
  private final @Nonnull File target;
  private boolean canceled = false;
  private long processedSpectra = 0;
  private Hashtable<String, Pattern> patterns;
  PriorityQueue<Pair<Double, Float>> mzIntensivePQ;

  @Nullable
  @Override
  public Collection<MgfMsSpectrum> execute() throws MSDKException {
    logger.info("Started MGF import from {} file", target);

    try (final BufferedReader reader = new BufferedReader(new FileReader(target))) {
      String line;
      while ((line = reader.readLine()) != null) {
        switch (line) {
          case "BEGIN IONS":
            spectra.add(processSpectrum(reader));
            processedSpectra++;
            break;
        }
      }
      reader.close();
    } catch (IOException e) {
      throw new MSDKException(e);
    }

    return spectra;
  }

  private MgfMsSpectrum processSpectrum(BufferedReader reader) throws IOException {
    String line;
    String title = "";
    int precursorCharge = 0;
    double precursorMass = 0;
    Matcher matcher = null;

    int matcherId = -1;
    String groupped;

    while (true) {

      line = reader.readLine();
      if (line == null || line.equals("END IONS"))
        break;

      if (line.contains("PEPMASS") || line.contains("TITLE") || line.contains("CHARGE")) {
        if (line.contains("PEPMASS")) {
          matcher = patterns.get("PEPMASS").matcher(line);
          matcherId = 1;
        } else if (line.contains("TITLE")) {
          matcher = patterns.get("TITLE").matcher(line);
          matcherId = 2;
        } else if (line.contains("CHARGE")) {
          matcher = patterns.get("CHARGE").matcher(line);
          matcherId = 3;
        }
        /* Do not like this code */
        if (matcher.find()) {
          groupped = matcher.group();
          switch (matcherId) {
            case 1:
              precursorMass = Double.parseDouble(groupped);
              break;
            case 2:
              title = groupped;
              break;
            case 3:
              String trimmed = groupped.substring(0, groupped.length() - 1);
              precursorCharge = Integer.parseInt(trimmed);
              if (groupped.charAt(groupped.length() - 1) == '-') {
                precursorCharge *= -1;
              }
              break;
            default:
              break;
          }
        }
      } else {
        String[] floats = line.split(" ");
        double mzValue = Double.parseDouble(floats[0]);
        float intensiveValue = Float.parseFloat(floats[1]);
        mzIntensivePQ.add(new Pair<Double, Float>(mzValue, intensiveValue));
      }
    }


    /* AbstractMsSpectrum requires sorted sequence of mz, PQ sorts values */
    int index = 0;
    double mz[] = new double[16];
    float intensive[] = new float[16];
    while (!mzIntensivePQ.isEmpty()){
      Pair<Double, Float> pair = mzIntensivePQ.remove();
      double mzValue = pair.getKey();
      float intensiveValue = pair.getValue();
      mz = ArrayUtil.addToArray(mz, mzValue, index);
      intensive = ArrayUtil.addToArray(intensive, intensiveValue, index);
      index++;
    }

    /*
     Do not like this code too
     May be implement a Builder pattern for this?
    */
    MgfMsSpectrum spectrum = new MgfMsSpectrum(mz, intensive, index - 1, title, precursorCharge,
        precursorMass, MsSpectrumType.CENTROIDED);

    return spectrum;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Float getFinishedPercentage() {
    int totalSpectra = spectra.size();
    return totalSpectra == 0 ? null : (float) (processedSpectra / totalSpectra);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public Collection<MgfMsSpectrum> getResult() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancel() {
    this.canceled = true;
  }

  public MgfFileImportMethod(File target) {
    MgfPairComparator comparator = new MgfPairComparator();
    mzIntensivePQ = new PriorityQueue<>(10, comparator);

    this.target = target;
    spectra = new LinkedList<>();
    patterns = new Hashtable<>();
    patterns.put("PEPMASS", Pattern.compile("(?<=PEPMASS=)(\\d+\\.\\d+)"));
    patterns.put("CHARGE", Pattern.compile("(?<=CHARGE=)(\\d+)\\+|-"));
    patterns.put("TITLE", Pattern.compile("(?<=TITLE=).*"));
  }
}
