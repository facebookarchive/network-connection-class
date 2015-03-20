/*
 *  Copyright (c) 2015, Facebook, Inc.
 *  All rights reserved.
 *
 *  This source code is licensed under the BSD-style license found in the
 *  LICENSE file in the root directory of this source tree. An additional grant
 *  of patent rights can be found in the PATENTS file in the same directory.
 *
 */

package com.facebook.network.connectionclass;

import android.os.StrictMode;
import android.util.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Class for parsing total number of downloaded bytes
 * from {@code /proc/net/xt_qtaguid/stats}.
 */
class QTagParser {
  private static final String TAG = "QTagParser";
  private static final String QTAGUID_UID_STATS = "/proc/net/xt_qtaguid/stats";

  private static final ThreadLocal<byte[]> sLineBuffer = new ThreadLocal<byte[]>() {
    @Override
    public byte[] initialValue() {
      return new byte[512];
    }
  };

  private static long sPreviousBytes = -1;
  private static LineBufferReader sStatsReader = new LineBufferReader();
  private static ByteArrayScanner sScanner = new ByteArrayScanner();

  @Nullable
  public static QTagParser sInstance;

  @Nonnull
  public static synchronized QTagParser getInstance() {
    if (sInstance == null) {
      sInstance = new QTagParser(QTAGUID_UID_STATS);
    }
      return sInstance;
  }

  private String mPath;

  // @VisibleForTesting
  public QTagParser(String path) {
    mPath = path;
  }

  /**
   * Reads the qtaguid file and returns a difference from the previous read.
   * @param uid The target uid to read bytes downloaded for.
   * @return The difference between the current number of bytes downloaded and
   */
  public long parseDataUsageForUidAndTag(int uid) {
    // The format of each line is
    // idx iface acct_tag_hex uid_tag_int cnt_set rx_bytes rx_packets tx_bytes
    // (There are many more fields but we are not interested in them)
    // For us parts: 1, 2, 3 are to see if the line is relevant
    // and part 5 is the received bytes
    // (part numbers start from 0)

    // Permit disk reads here, as /proc/net/xt_qtaguid/stats isn't really "on
    // disk" and should be fast.
    StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
    try {
      long tagRxBytes = 0;

      FileInputStream fis = new FileInputStream(mPath);
      sStatsReader.setFileStream(fis);
      byte[] buffer = sLineBuffer.get();

      try {
        int length;
        sStatsReader.skipLine(); // skip first line (headers)

        int line = 2;
        while ((length = sStatsReader.readLine(buffer)) != -1) {
          try {

            // Content is arranged in terms of:
            // idx iface acct_tag_hex uid_tag_int cnt_set rx_bytes rx_packets tx_bytes tx_packets rx_tcp_bytes
            // rx_tcp_packets rx_udp_bytes rx_udp_packets rx_other_bytes rx_other_packets tx_tcp_bytes tx_tcp_packets
            // tx_udp_bytes tx_udp_packets tx_other_bytes tx_other_packets

            // The ones we're interested in are:
            // idx - ignore
            // interface, filter out local interface ("lo")
            // tag - ignore
            // uid_tag_int, match it with the UID of interest
            // cnt_set - ignore
            // rx_bytes

            sScanner.reset(buffer, length);
            sScanner.useDelimiter(' ');

            sScanner.skip();
            if (sScanner.nextStringEquals("lo")) {
              continue;
            }
            sScanner.skip();
            if (sScanner.nextInt() != uid) {
              continue;
            }
            sScanner.skip();
            int rxBytes = sScanner.nextInt();
            tagRxBytes += rxBytes;
            line++;
            // If the line is incorrectly formatted, ignore the line.
          } catch (NumberFormatException e) {
            Log.e(TAG, "Cannot parse byte count at line" + line + ".");
            continue;
          } catch (NoSuchElementException e) {
            Log.e(TAG, "Invalid number of tokens on line " + line + ".");
            continue;
          }
        }
      } finally {
        fis.close();
      }

      if (sPreviousBytes == -1) {
        sPreviousBytes = tagRxBytes;
        return -1;
      }
      long diff = tagRxBytes - sPreviousBytes;
      sPreviousBytes = tagRxBytes;
      return diff;

    } catch (IOException e) {
      Log.e (TAG, "Error reading from /proc/net/xt_qtaguid/stats. Please check if this file exists.");
    } finally {
      StrictMode.setThreadPolicy(savedPolicy);
    }

    // Return -1 upon error.
    return -1;
  }
}