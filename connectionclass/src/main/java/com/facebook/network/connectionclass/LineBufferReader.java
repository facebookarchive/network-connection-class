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

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Class for reading {@code /proc/net/xt_qtaguid/stats} line by line with a small,
 * reusable byte buffer.
 */
class LineBufferReader {

  private byte[] mFileBuffer;
  private FileInputStream mInputStream;
  private int mFileBufIndex;
  private int mBytesInBuffer;

  public LineBufferReader() {
    mFileBuffer = new byte[512];
  }

  /**
   * Sets the FileInputStream for reading.
   * @param is The FileInputStream to set.
   */
  public void setFileStream (FileInputStream is) {
    mInputStream = is;
    mBytesInBuffer = 0;
    mFileBufIndex = 0;
  }

  /**
   * @param lineBuffer The buffer to fill with the current line.
   * @return The index in the buffer at which the line terminates.
   */
  public int readLine(byte[] lineBuffer)
      throws IOException{
    if (mFileBufIndex >= mBytesInBuffer) {
      mBytesInBuffer = mInputStream.read(mFileBuffer);
      mFileBufIndex = 0;
    }
    int i;
    for (i = 0; mBytesInBuffer != -1 && i < lineBuffer.length
        && mFileBuffer[mFileBufIndex] != '\n'; i++) {
      lineBuffer[i] = mFileBuffer[mFileBufIndex];
      mFileBufIndex++;
      if (mFileBufIndex >= mBytesInBuffer) {
        mBytesInBuffer = mInputStream.read(mFileBuffer);
        mFileBufIndex = 0;
      }
    }
     // Move past the newline character.
    mFileBufIndex++;
     // If there are no more bytes to be read into the buffer,
     // we have reached the end of this file. Exit.
    if (mBytesInBuffer == -1) {
      return -1;
    }
    return i;
  }

  /**
   * Skips a line in the current file stream.
   */
  public void skipLine()
      throws IOException {
    if (mFileBufIndex >= mBytesInBuffer) {
      mBytesInBuffer = mInputStream.read(mFileBuffer);
      mFileBufIndex = 0;
    }
    for (int i = 0; mBytesInBuffer != -1 && mFileBuffer[mFileBufIndex] != '\n'; i++) {
      mFileBufIndex++;
      if (mFileBufIndex >= mBytesInBuffer) {
        mBytesInBuffer = mInputStream.read(mFileBuffer);
        mFileBufIndex = 0;
      }
    }
    // Move past the newline character.
    mFileBufIndex++;
  }
}
