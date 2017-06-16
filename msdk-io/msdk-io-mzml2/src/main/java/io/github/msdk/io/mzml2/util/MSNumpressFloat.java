/*
 * MSNumpress.java johan.teleman@immun.lth.se
 * 
 * Copyright 2013 Johan Teleman
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.msdk.io.mzml2.util;

public class MSNumpressFloat {

  /// PSI-MS obo accession numbers.
  public static final String ACC_NUMPRESS_LINEAR = "MS:1002312";
  public static final String ACC_NUMPRESS_PIC = "MS:1002313";
  public static final String ACC_NUMPRESS_SLOF = "MS:1002314";


  /**
   * Convenience function for decoding binary data encoded by MSNumpress. If the passed cvAccession
   * is one of
   * <p/>
   * ACC_NUMPRESS_LINEAR = "MS:1002312" ACC_NUMPRESS_PIC = "MS:1002313" ACC_NUMPRESS_SLOF =
   * "MS:1002314"
   * <p/>
   * the corresponding decode function will be called.
   *
   * @param cvAccession The PSI-MS obo CV accession of the encoded data.
   * @param data array of float to be encoded
   * @param dataSize number of floats from data to encode
   * @return The decoded floats
   */
  public static float[] decode(String cvAccession, byte[] data, int dataSize) {

    switch (cvAccession) {
      case ACC_NUMPRESS_LINEAR: {
        float[] buffer = new float[dataSize * 2];
        int nbrOfFloats = MSNumpressFloat.decodeLinear(data, dataSize, buffer);
        float[] result = new float[nbrOfFloats];
        System.arraycopy(buffer, 0, result, 0, nbrOfFloats);
        return result;

      }
      case ACC_NUMPRESS_SLOF: {
        float[] result = new float[dataSize / 2];
        MSNumpressFloat.decodeSlof(data, dataSize, result);
        return result;

      }
      case ACC_NUMPRESS_PIC: {
        float[] buffer = new float[dataSize * 2];
        int nbrOfFloats = MSNumpressFloat.decodePic(data, dataSize, buffer);
        float[] result = new float[nbrOfFloats];
        System.arraycopy(buffer, 0, result, 0, nbrOfFloats);
        return result;

      }
    }

    throw new IllegalArgumentException("'" + cvAccession + "' is not a numpress compression term");
  }


  /**
   * This encoding works on a 4 byte integer, by truncating initial zeros or ones. If the initial
   * (most significant) half byte is 0x0 or 0xf, the number of such halfbytes starting from the most
   * significant is stored in a halfbyte. This initial count is then followed by the rest of the
   * ints halfbytes, in little-endian order. A count halfbyte c of
   * <p/>
   * 0 <= c <= 8 is interpreted as an initial c 0x0 halfbytes 9 <= c <= 15 is interpreted as an
   * initial (c-8) 0xf halfbytes
   * <p/>
   * Ex: int c rest 0 => 0x8 -1 => 0xf 0xf 23 => 0x6 0x7 0x1
   *
   * @param x the int to be encoded
   * @param res the byte array were halfbytes are stored
   * @param resOffset position in res were halfbytes are written
   * @return the number of resulting halfbytes
   */
  protected static int encodeInt(long x, byte[] res, int resOffset) {
    byte i, l;
    long m;
    long mask = 0xf0000000;
    long init = x & mask;

    if (init == 0) {
      l = 8;
      for (i = 0; i < 8; i++) {
        m = mask >> (4 * i);
        if ((x & m) != 0) {
          l = i;
          break;
        }
      }
      res[resOffset] = l;
      for (i = l; i < 8; i++)
        res[resOffset + 1 + i - l] = (byte) (0xf & (x >> (4 * (i - l))));

      return 1 + 8 - l;

    } else if (init == mask) {
      l = 7;
      for (i = 0; i < 8; i++) {
        m = mask >> (4 * i);
        if ((x & m) != m) {
          l = i;
          break;
        }
      }
      res[resOffset] = (byte) (l | 8);
      for (i = l; i < 8; i++)
        res[resOffset + 1 + i - l] = (byte) (0xf & (x >> (4 * (i - l))));

      return 1 + 8 - l;

    } else {
      res[resOffset] = 0;
      for (i = 0; i < 8; i++)
        res[resOffset + 1 + i] = (byte) (0xf & (x >> (4 * i)));

      return 9;

    }
  }


  public static void encodeFixedPoint(float fixedPoint, byte[] result) {
    long fp = Double.doubleToLongBits(fixedPoint);
    for (int i = 0; i < 8; i++) {
      result[7 - i] = (byte) ((fp >> (8 * i)) & 0xff);
    }
  }


  public static float decodeFixedPoint(byte[] data) {
    long fp = 0;
    for (int i = 0; i < 8; i++) {
      fp = fp | ((0xFFl & data[7 - i]) << (8 * i));
    }
    return (float) Double.longBitsToDouble(fp);
  }


  /////////////////////////////////////////////////////////////////////////////////


  public static float optimalLinearFixedPoint(float[] data, int dataSize) {
    if (dataSize == 0)
      return 0;
    if (dataSize == 1)
      return (float) Math.floor(0xFFFFFFFFl / data[0]);
    float maxFloat = Math.max(data[0], data[1]);

    for (int i = 2; i < dataSize; i++) {
      float extrapol = data[i - 1] + (data[i - 1] - data[i - 2]);
      float diff = data[i] - extrapol;
      maxFloat = (float) Math.max(maxFloat, Math.ceil(Math.abs(diff) + 1));
    }

    return (float) Math.floor(0x7FFFFFFFl / maxFloat);
  }


  /**
   * Encodes the floats in data by first using a - lossy conversion to a 4 byte 5 decimal fixed
   * point repressentation - storing the residuals from a linear prediction after first to values -
   * encoding by encodeInt (see above)
   * <p/>
   * The resulting binary is maximally 8 + dataSize * 5 bytes, but much less if the data is
   * reasonably smooth on the first order.
   * <p/>
   * This encoding is suitable for typical m/z or retention time binary arrays. On a test set, the
   * encoding was empirically show to be accurate to at least 0.002 ppm.
   *
   * @param data array of floats to be encoded
   * @param dataSize number of floats from data to encode
   * @param result array were resulting bytes should be stored
   * @param fixedPoint the scaling factor used for getting the fixed point repr. This is stored in
   *        the binary and automatically extracted on decoding.
   * @return the number of encoded bytes
   */
  public static int encodeLinear(float[] data, int dataSize, byte[] result, float fixedPoint) {
    long[] ints = new long[3];
    int i, ri, halfByteCount, hbi;
    byte halfBytes[] = new byte[10];
    long extrapol, diff;

    encodeFixedPoint(fixedPoint, result);

    if (dataSize == 0)
      return 8;

    ints[1] = (long) (data[0] * fixedPoint + 0.5);
    for (i = 0; i < 4; i++) {
      result[8 + i] = (byte) ((ints[1] >> (i * 8)) & 0xff);
    }

    if (dataSize == 1)
      return 12;

    ints[2] = (long) (data[1] * fixedPoint + 0.5);
    for (i = 0; i < 4; i++) {
      result[12 + i] = (byte) ((ints[2] >> (i * 8)) & 0xff);
    }

    halfByteCount = 0;
    ri = 16;

    for (i = 2; i < dataSize; i++) {
      ints[0] = ints[1];
      ints[1] = ints[2];
      ints[2] = (long) (data[i] * fixedPoint + 0.5);
      extrapol = ints[1] + (ints[1] - ints[0]);
      diff = ints[2] - extrapol;
      halfByteCount += encodeInt(diff, halfBytes, halfByteCount);

      for (hbi = 1; hbi < halfByteCount; hbi += 2)
        result[ri++] = (byte) ((halfBytes[hbi - 1] << 4) | (halfBytes[hbi] & 0xf));

      if (halfByteCount % 2 != 0) {
        halfBytes[0] = halfBytes[halfByteCount - 1];
        halfByteCount = 1;
      } else
        halfByteCount = 0;

    }
    if (halfByteCount == 1)
      result[ri++] = (byte) (halfBytes[0] << 4);

    return ri;
  }


  /**
   * Decodes data encoded by encodeLinear.
   * <p/>
   * result vector guaranteed to be shorter or equal to (|data| - 8) * 2
   * <p/>
   * Note that this method may throw a ArrayIndexOutOfBoundsException if it deems the input data to
   * be corrupt, i.e. that the last encoded int does not use the last byte in the data. In addition
   * the last encoded int need to use either the last halfbyte, or the second last followed by a 0x0
   * halfbyte.
   *
   * @param data array of bytes to be decoded
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting floats should be stored
   * @return the number of decoded floats, or -1 if dataSize < 4 or 4 < dataSize < 8
   */
  public static int decodeLinear(byte[] data, int dataSize, float[] result) {
    int ri = 2;
    long[] ints = new long[3];
    long extrapol;
    long y;
    IntDecoder dec = new IntDecoder(data, 16);

    if (dataSize < 8)
      return -1;
    float fixedPoint = decodeFixedPoint(data);
    if (dataSize < 12)
      return -1;

    ints[1] = 0;
    for (int i = 0; i < 4; i++) {
      ints[1] = ints[1] | ((0xFFl & data[8 + i]) << (i * 8));
    }
    result[0] = ints[1] / fixedPoint;

    if (dataSize == 12)
      return 1;
    if (dataSize < 16)
      return -1;

    ints[2] = 0;
    for (int i = 0; i < 4; i++) {
      ints[2] = ints[2] | ((0xFFl & data[12 + i]) << (i * 8));
    }
    result[1] = ints[2] / fixedPoint;

    while (dec.pos < dataSize) {
      if (dec.pos == (dataSize - 1) && dec.half)
        if ((data[dec.pos] & 0xf) != 0x8)
          break;

      ints[0] = ints[1];
      ints[1] = ints[2];
      ints[2] = dec.next();

      extrapol = ints[1] + (ints[1] - ints[0]);
      y = extrapol + ints[2];
      result[ri++] = y / fixedPoint;
      ints[2] = y;
    }

    return ri;
  }


  /////////////////////////////////////////////////////////////////////////////////

  /**
   * Encodes ion counts by simply rounding to the nearest 4 byte integer, and compressing each
   * integer with encodeInt.
   * <p/>
   * The handleable range is therefore 0 -> 4294967294. The resulting binary is maximally dataSize *
   * 5 bytes, but much less if the data is close to 0 on average.
   *
   * @param data array of floats to be encoded
   * @param dataSize number of floats from data to encode
   * @param result array were resulting bytes should be stored
   * @return the number of encoded bytes
   */
  public static int encodePic(float[] data, int dataSize, byte[] result) {
    long count;
    int ri = 0;
    int hbi;
    byte halfBytes[] = new byte[10];
    int halfByteCount = 0;

    // printf("Encoding %d floats\n", (int)dataSize);

    for (int i = 0; i < dataSize; i++) {
      count = (long) (data[i] + 0.5);
      halfByteCount += encodeInt(count, halfBytes, halfByteCount);

      for (hbi = 1; hbi < halfByteCount; hbi += 2)
        result[ri++] = (byte) ((halfBytes[hbi - 1] << 4) | (halfBytes[hbi] & 0xf));

      if (halfByteCount % 2 != 0) {
        halfBytes[0] = halfBytes[halfByteCount - 1];
        halfByteCount = 1;
      } else
        halfByteCount = 0;

    }
    if (halfByteCount == 1)
      result[ri++] = (byte) (halfBytes[0] << 4);

    return ri;
  }


  /**
   * Decodes data encoded by encodePic
   * <p/>
   * result vector guaranteed to be shorter of equal to |data| * 2
   * <p/>
   * Note that this method may throw a ArrayIndexOutOfBoundsException if it deems the input data to
   * be corrupt, i.e. that the last encoded int does not use the last byte in the data. In addition
   * the last encoded int need to use either the last halfbyte, or the second last followed by a 0x0
   * halfbyte.
   *
   * @param data array of bytes to be decoded (need memorycont. repr.)
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting floats should be stored
   * @return the number of decoded floats
   */
  public static int decodePic(byte[] data, int dataSize, float[] result) {
    int ri = 0;
    long count;
    IntDecoder dec = new IntDecoder(data, 0);

    while (dec.pos < dataSize) {
      if (dec.pos == (dataSize - 1) && dec.half)
        if ((data[dec.pos] & 0xf) != 0x8)
          break;

      count = dec.next();
      result[ri++] = count;
    }
    return ri;
  }


  /////////////////////////////////////////////////////////////////////////////////

  public static float optimalSlofFixedPoint(float[] data, int dataSize) {
    if (dataSize == 0)
      return 0;

    float maxFloat = 1;
    float x;
    float fp;

    for (int i = 0; i < dataSize; i++) {
      x = (float) Math.log(data[i] + 1);
      maxFloat = Math.max(maxFloat, x);
    }

    fp = (float) Math.floor(0xFFFF / maxFloat);

    return fp;
  }

  /**
   * Encodes ion counts by taking the natural logarithm, and storing a fixed point representation of
   * this. This is calculated as
   * <p/>
   * unsigned short fp = log(d+1) * fixedPoint + 0.5
   * <p/>
   * the result vector is exactly |data| * 2 + 8 bytes long
   *
   * @param data array of floats to be encoded
   * @param dataSize number of floats from data to encode
   * @param result array were resulting bytes should be stored
   * @param fixedPoint the scaling factor used for getting the fixed point repr. This is stored in
   *        the binary and automatically extracted on decoding.
   * @return the number of encoded bytes
   */
  public static int encodeSlof(float[] data, int dataSize, byte[] result, float fixedPoint) {
    int x;
    int ri = 8;

    encodeFixedPoint(fixedPoint, result);

    for (int i = 0; i < dataSize; i++) {
      x = (int) (Math.log(data[i] + 1) * fixedPoint + 0.5);

      result[ri++] = (byte) (0xff & x);
      result[ri++] = (byte) (x >> 8);
    }
    return ri;
  }


  /**
   * Decodes data encoded by encodeSlof
   * <p/>
   * The result vector will be exactly (|data| - 8) / 2 floats. returns the number of floats read,
   * or -1 is there is a problem decoding.
   *
   * @param data array of bytes to be decoded (need memorycont. repr.)
   * @param dataSize number of bytes from data to decode
   * @param result array were resulting floats should be stored
   * @return the number of decoded floats
   */
  public static int decodeSlof(byte[] data, int dataSize, float[] result) {
    int x;
    int ri = 0;

    if (dataSize < 8)
      return -1;
    float fixedPoint = decodeFixedPoint(data);

    if (dataSize % 2 != 0)
      return -1;

    for (int i = 8; i < dataSize; i += 2) {
      x = (0xff & data[i]) | ((0xff & data[i + 1]) << 8);
      result[ri++] = (float) (Math.exp(((float) (0xffff & x)) / fixedPoint) - 1);
    }
    return ri;
  }
}
