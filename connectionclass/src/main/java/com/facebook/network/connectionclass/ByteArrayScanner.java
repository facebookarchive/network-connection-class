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

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

class ByteArrayScanner {
  private @Nullable byte[] mData;
  private int mCurrentOffset;
  private int mTotalLength;
  private char mDelimiter;
  private boolean mDelimiterSet;

  public ByteArrayScanner reset(byte[] buffer, int length) {
    mData = buffer;
    mCurrentOffset = 0;
    mTotalLength = length;
    mDelimiterSet = false;
    return this;
  }

  public ByteArrayScanner useDelimiter(char delimiter) {
    throwIfNotReset();
    mDelimiter = delimiter;
    mDelimiterSet = true;
    return this;
  }

  private void throwIfNotReset() {
    if (mData == null) {
      throw new IllegalStateException("Must call reset first");
    }
  }

  private void throwIfDelimiterNotSet() {
    if (!mDelimiterSet) {
      throw new IllegalStateException("Must call useDelimiter first");
    }
  }

  /**
   * @return The next token, parsed as a string.
   * @throws NoSuchElementException
   */
  public String nextString()
      throws NoSuchElementException {
    throwIfNotReset();
    throwIfDelimiterNotSet();
    int offset = mCurrentOffset;
    int length = advance();
    return new String(mData, offset, length);
  }

  /**
   * Matches the next token with a string.
   * @param str String to match the next token with.
   * @return True if the next token matches, false otherwise.
   * @throws NoSuchElementException
   */
  public boolean nextStringEquals(String str)
      throws NoSuchElementException {
    int offset = mCurrentOffset;
    int length = advance();
    if (str.length() != length) {
      return false;
    }
    for (int i = 0; i < str.length(); i++) {
      if (str.charAt(i) != mData[offset]) {
        return false;
      }
      offset++;
    }
    return true;
  }

  /**
   * @return The next token, parsed as an integer.
   * @throws NoSuchElementException
   */
  public int nextInt()
      throws NoSuchElementException{
    throwIfNotReset();
    throwIfDelimiterNotSet();
    int offset = mCurrentOffset;
    int length = advance();
    int value = parseInt(
        mData,
        offset,
        offset + length);
    return value;
  }

  /**
   * Move to the next token.
   * @throws NoSuchElementException
   */
  public void skip()
      throws NoSuchElementException {
    throwIfNotReset();
    throwIfDelimiterNotSet();
    advance();
  }

  private int advance()
      throws NoSuchElementException {
    throwIfNotReset();
    throwIfDelimiterNotSet();
    if (mTotalLength <= mCurrentOffset) {
      throw new NoSuchElementException("Reading past end of input stream at " + mCurrentOffset + ".");
    }
    int index = indexOf(
        mData,
        mCurrentOffset,
        mTotalLength,
        mDelimiter);
    if (index == -1) {
      int length = mTotalLength - mCurrentOffset;
      mCurrentOffset = mTotalLength;
      return length;
    } else {
      int length = index - mCurrentOffset;
      mCurrentOffset = index + 1;
      return length;
    }
  }

  private static int parseInt(byte[] buffer, int start, int end)
      throws NumberFormatException {
    int radix = 10;
    int result = 0;
    while (start < end) {
      int digit = buffer[start++] - '0';
      if (digit < 0 || digit > 9) {
        throw new NumberFormatException("Invalid int in buffer at " + (start - 1) + ".");
      }
      int next = result * radix + digit;
      result = next;
    }
    return result;
  }

  private static int indexOf(byte[] data, int start, int end, char ch) {
    for (int i = start; i < end; i++) {
      if (data[i] == ch) {
        return i;
      }
    }
    return -1;
  }
}
