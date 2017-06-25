/*
 * (C) Copyright 2015-2016 by MSDK Development Team
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

package io.github.msdk.io.mzml2.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;

import com.google.common.io.LittleEndianDataInputStream;

import io.github.msdk.MSDKException;
import io.github.msdk.io.mzml2.data.MzMLBinaryDataInfo;
import it.unimi.dsi.io.ByteBufferInputStream;

/**
 * <p>
 * MzMLIntensityPeaksDecoder class.
 * </p>
 *
 */
public class MzMLPeaksDecoder {

  /**
   * Converts a base64 encoded mz or intensity string used in mzML files to an array of floats. If
   * the original precision was 64 bit, you still get floats as output.
   *
   * @param InputStream, decoded from a base64 encoded string<br>
   *        E.g. like: eNoNxltIkwEYBuAOREZFhrCudGFbbraTU+Zmue...
   * @param lengthIn length of data to be treated as values, i.e. the input array can be longer, the
   *        values to be interpreted must start at offset 0, and this will indicate the length
   * @param precision allowed values: 32 and 64, can be null only if MS-NUMPRESS compression was
   *        applied and is specified in the @{code compressions} enum set.
   * @param numPoints a int.
   * @param compression null or MzMLCompressionType#NO_COMPRESSION have the same effect. Otherwise
   *        the binary data will be inflated according to the compression rules.
   * @throws java.util.zip.DataFormatException if any.
   * @throws java.io.IOException if any.
   * @return a float array containing the decoded values
   * @throws io.github.msdk.MSDKException if any.
   */
  public static float[] decodeToFloat(ByteBufferInputStream mappedByteBufferInputStream,
      MzMLBinaryDataInfo binaryDataInfo) throws DataFormatException, IOException, MSDKException {

    int lengthIn = binaryDataInfo.getEncodedLength();
    int numPoints = binaryDataInfo.getArrayLength();

    InputStream encodedIs = new ByteBufferInputStreamAdapter(mappedByteBufferInputStream,
        binaryDataInfo.getPosition(), lengthIn);
    InputStream is = Base64.getDecoder().wrap(encodedIs);

    // for some reason there sometimes might be zero length <peaks> tags
    // (ms2 usually)
    // in this case we just return an empty result
    if (lengthIn == 0) {
      return new float[0];
    }

    InflaterInputStream iis = null;
    LittleEndianDataInputStream dis = null;
    byte[] bytes = null;

    float[] data = new float[numPoints];

    // first check for zlib compression, inflation must be done before
    // NumPress
    if (binaryDataInfo.getCompressionType() != null) {
      switch (binaryDataInfo.getCompressionType()) {
        case ZLIB:
        case NUMPRESS_LINPRED_ZLIB:
        case NUMPRESS_POSINT_ZLIB:
        case NUMPRESS_SHLOGF_ZLIB:
          iis = new InflaterInputStream(is);
          dis = new LittleEndianDataInputStream(iis);
          break;
        default:
          dis = new LittleEndianDataInputStream(is);
          break;
      }

      // Now we can check for NumPress
      int numDecodedDoubles;
      switch (binaryDataInfo.getCompressionType()) {
        case NUMPRESS_LINPRED:
        case NUMPRESS_LINPRED_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodeLinear(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress linear decoder failed");
          }
          return data;
        case NUMPRESS_POSINT:
        case NUMPRESS_POSINT_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodePic(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress positive integer decoder failed");
          }
          return data;
        case NUMPRESS_SHLOGF:
        case NUMPRESS_SHLOGF_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodeSlof(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress short logged float decoder failed");
          }
          return data;
        default:
          break;
      }
    } else {
      dis = new LittleEndianDataInputStream(is);
    }

    Integer precision;
    switch (binaryDataInfo.getBitLength()) {
      case THIRTY_TWO_BIT_FLOAT:
      case THIRTY_TWO_BIT_INTEGER:
        precision = 32;
        break;
      case SIXTY_FOUR_BIT_FLOAT:
      case SIXTY_FOUR_BIT_INTEGER:
        precision = 64;
        break;
      default:
        dis.close();
        throw new IllegalArgumentException(
            "Precision MUST be specified and be either 32-bit or 64-bit, "
                + "if MS-NUMPRESS compression was not used");
    }

    switch (precision) {
      case (32): {
        int asInt;

        for (int i = 0; i < numPoints; i++) {
          asInt = dis.readInt();
          data[i] = Float.intBitsToFloat(asInt);
        }
        break;
      }
      case (64): {
        long asLong;

        for (int i = 0; i < numPoints; i++) {
          asLong = dis.readLong();
          data[i] = (float) Double.longBitsToDouble(asLong);
        }
        break;
      }
      default: {
        dis.close();
        throw new IllegalArgumentException(
            "Precision can only be 32/64 bits, other values are not valid.");
      }
    }

    dis.close();
    return data;
  }

  /**
   * Converts a base64 encoded mz or intensity string used in mzML files to an array of doubles. If
   * the original precision was 32 bit, you still get doubles as output.
   *
   * @param InputStream, decoded from a base64 encoded string<br>
   *        E.g. like: eNoNxltIkwEYBuAOREZFhrCudGFbbraTU+Zmue...
   * @param lengthIn length of data to be treated as values, i.e. the input array can be longer, the
   *        values to be interpreted must start at offset 0, and this will indicate the length
   * @param precision allowed values: 32 and 64, can be null only if MS-NUMPRESS compression was
   *        applied and is specified in the @{code compressions} enum set.
   * @param numPoints a int.
   * @param compression null or MzMLCompressionType#NO_COMPRESSION have the same effect. Otherwise
   *        the binary data will be inflated according to the compression rules.
   * @throws java.util.zip.DataFormatException if any.
   * @throws java.io.IOException if any.
   * @return a double array containing the decoded values
   * @throws io.github.msdk.MSDKException if any.
   */
  public static double[] decodeToDouble(ByteBufferInputStream mappedByteBufferInputStream,
      MzMLBinaryDataInfo binaryDataInfo) throws DataFormatException, IOException, MSDKException {

    int lengthIn = binaryDataInfo.getEncodedLength();
    int numPoints = binaryDataInfo.getArrayLength();

    InputStream encodedIs = new ByteBufferInputStreamAdapter(mappedByteBufferInputStream,
        binaryDataInfo.getPosition(), lengthIn);
    InputStream is = Base64.getDecoder().wrap(encodedIs);

    // for some reason there sometimes might be zero length <peaks> tags
    // (ms2 usually)
    // in this case we just return an empty result
    if (lengthIn == 0) {
      return new double[0];
    }

    InflaterInputStream iis = null;
    LittleEndianDataInputStream dis = null;
    byte[] bytes = null;

    double[] data = new double[numPoints];

    // first check for zlib compression, inflation must be done before
    // NumPress
    if (binaryDataInfo.getCompressionType() != null) {
      switch (binaryDataInfo.getCompressionType()) {
        case ZLIB:
        case NUMPRESS_LINPRED_ZLIB:
        case NUMPRESS_POSINT_ZLIB:
        case NUMPRESS_SHLOGF_ZLIB:
          iis = new InflaterInputStream(is);
          dis = new LittleEndianDataInputStream(iis);
          break;
        default:
          dis = new LittleEndianDataInputStream(is);
          break;
      }

      // Now we can check for NumPress
      int numDecodedDoubles;
      switch (binaryDataInfo.getCompressionType()) {
        case NUMPRESS_LINPRED:
        case NUMPRESS_LINPRED_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodeLinear(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress linear decoder failed");
          }
          return data;
        case NUMPRESS_POSINT:
        case NUMPRESS_POSINT_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodePic(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress positive integer decoder failed");
          }
          return data;
        case NUMPRESS_SHLOGF:
        case NUMPRESS_SHLOGF_ZLIB:
          bytes = IOUtils.toByteArray(dis);
          numDecodedDoubles = MSNumpress.decodeSlof(bytes, bytes.length, data);
          if (numDecodedDoubles < 0) {
            throw new MSDKException("MSNumpress short logged float decoder failed");
          }
          return data;
        default:
          break;
      }
    } else {
      dis = new LittleEndianDataInputStream(is);
    }

    Integer precision;
    switch (binaryDataInfo.getBitLength()) {
      case THIRTY_TWO_BIT_FLOAT:
      case THIRTY_TWO_BIT_INTEGER:
        precision = 32;
        break;
      case SIXTY_FOUR_BIT_FLOAT:
      case SIXTY_FOUR_BIT_INTEGER:
        precision = 64;
        break;
      default:
        dis.close();
        throw new IllegalArgumentException(
            "Precision MUST be specified and be either 32-bit or 64-bit, "
                + "if MS-NUMPRESS compression was not used");
    }

    switch (precision) {
      case (32): {
        int asInt;

        for (int i = 0; i < numPoints; i++) {
          asInt = dis.readInt();
          data[i] = Float.intBitsToFloat(asInt);
        }
        break;
      }
      case (64): {
        long asLong;

        for (int i = 0; i < numPoints; i++) {
          asLong = dis.readLong();
          data[i] = Double.longBitsToDouble(asLong);
        }
        break;
      }
      default: {
        dis.close();
        throw new IllegalArgumentException(
            "Precision can only be 32/64 bits, other values are not valid.");
      }
    }

    dis.close();
    return data;
  }

}
