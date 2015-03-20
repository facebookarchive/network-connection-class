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
import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.QTagParser;
import dalvik.annotation.TestTargetClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
public class ConnectionClassTest {

  @Mock
  public ConnectionClassManager mConnectionClassManager;
  public TestBandwidthStateChangeListener mTestBandwidthStateChangeListener;

  @Before
  public void setUp() {
    mConnectionClassManager = ConnectionClassManager.getInstance();
    mTestBandwidthStateChangeListener = new TestBandwidthStateChangeListener();
  }

  //Test the moving average to make sure correct results are returned.
  @Test
  public void TestMovingAverage() {
    mConnectionClassManager.reset();
    mConnectionClassManager.addBandwidth(620000L, 1000L);
    mConnectionClassManager.addBandwidth(630000L, 1000L);
    mConnectionClassManager.addBandwidth(670000L, 1000L);
    mConnectionClassManager.addBandwidth(500000L, 1000L);
    mConnectionClassManager.addBandwidth(550000L, 1000L);
    mConnectionClassManager.addBandwidth(590000L, 1000L);
    assertEquals(ConnectionQuality.EXCELLENT, mConnectionClassManager.getCurrentBandwidthQuality());
  }

  //Test that values under the lower bandwidth bound do not affect the final ConnectionClass values.
  @Test
  public void TestGarbageValues() {
    mConnectionClassManager.reset();
    mConnectionClassManager.addBandwidth(620000L, 1000L);
    mConnectionClassManager.addBandwidth(0L, 1000L);
    mConnectionClassManager.addBandwidth(630000L, 1000L);
    mConnectionClassManager.addBandwidth(5L, 1000L);
    mConnectionClassManager.addBandwidth(10L, 1000L);
    mConnectionClassManager.addBandwidth(0L, 1000L);
    mConnectionClassManager.addBandwidth(90L, 1000L);
    mConnectionClassManager.addBandwidth(200L, 1000L);
    mConnectionClassManager.addBandwidth(670000L, 1000L);
    mConnectionClassManager.addBandwidth(500000L, 1000L);
    mConnectionClassManager.addBandwidth(550000L, 1000L);
    mConnectionClassManager.addBandwidth(590000L, 1000L);
    assertEquals(ConnectionQuality.EXCELLENT, mConnectionClassManager.getCurrentBandwidthQuality());
  }

  @Test
  public void testStateChangeBroadcastNoBroadcast() {
    mConnectionClassManager.reset();
    for (int i = 0; i < ConnectionClassManager.DEFAULT_SAMPLES_TO_QUALITY_CHANGE - 1; i++) {
      mConnectionClassManager.addBandwidth(1000, 2);
    }
    assertEquals(0, mTestBandwidthStateChangeListener.getNumberOfStateChanges());
  }

  @Test
  public void testStateChangeBroadcastWithBroadcast() {
    mConnectionClassManager.reset();
    for (int i = 0; i < ConnectionClassManager.DEFAULT_SAMPLES_TO_QUALITY_CHANGE + 1; i++) {
      mConnectionClassManager.addBandwidth(1000, 2);
    }
    assertEquals(1, mTestBandwidthStateChangeListener.getNumberOfStateChanges());
    assertEquals(ConnectionQuality.EXCELLENT, mTestBandwidthStateChangeListener.getLastBandwidthState());
  }

  @Test
  public void testStateChangeBroadcastDoesNotRepeatItself() {
    mConnectionClassManager.reset();
    for (int i = 0; i < 3 * ConnectionClassManager.DEFAULT_SAMPLES_TO_QUALITY_CHANGE + 1; i++) {
      mConnectionClassManager.addBandwidth(1000, 2);
    }
    assertEquals(1, mTestBandwidthStateChangeListener.getNumberOfStateChanges());
  }

  @PrepareForTest(StrictMode.class)
  @Test
  public void testParserWithExpectedInput()
      throws Exception {
    int testUid = 10237;
    PowerMockito.mockStatic(StrictMode.class);
    PowerMockito.doNothing().when(StrictMode.class, "setThreadPolicy", Matchers.anyObject());
    QTagParser testParser =
        new QTagParser("src/test/java/com/facebook/network/connectionclass/sampleqtaguidstats.txt");
    long result = testParser.parseDataUsageForUidAndTag(testUid);
    assertEquals(-1, result);
    result = testParser.parseDataUsageForUidAndTag(testUid);
    assertEquals(0, result);
  }

  @PrepareForTest({StrictMode.class,Log.class})
  @Test
  public void testParserDoesNotCrashOnWrongNumberOfTokens()
      throws Exception {
    PowerMockito.mockStatic(StrictMode.class);
    PowerMockito.doNothing().when(StrictMode.class, "setThreadPolicy", Matchers.anyObject());
    PowerMockito.mockStatic(Log.class);
    PowerMockito.doReturn(1).when(Log.class, "e", Matchers.anyString(), Matchers.anyString());
    QTagParser testParser =
        new QTagParser("src/test/java/com/facebook/network/connectionclass/strangelytokenedfile.txt");
    testParser.parseDataUsageForUidAndTag(0);
  }

  @PrepareForTest({StrictMode.class,Log.class})
  @Test
  public void testParserDoesNotCrashOnInvalidRxBytes()
      throws Exception {
    PowerMockito.mockStatic(StrictMode.class);
    PowerMockito.doNothing().when(StrictMode.class, "setThreadPolicy", Matchers.anyObject());
    PowerMockito.mockStatic(Log.class);
    PowerMockito.doReturn(1).when(Log.class, "e", Matchers.anyString(), Matchers.anyString());
    QTagParser testParser =
        new QTagParser("src/test/java/com/facebook/network/connectionclass/invalidrxbytes.txt");
    testParser.parseDataUsageForUidAndTag(0);
  }

  private class TestBandwidthStateChangeListener implements
      ConnectionClassManager.ConnectionClassStateChangeListener {

    private int mNumberOfStateChanges = 0;
    private ConnectionQuality mLastBandwidthState;

    private TestBandwidthStateChangeListener() {
      mConnectionClassManager.register(this);
    }

    @Override
    public void onBandwidthStateChange(ConnectionQuality bandwidthState) {
      mNumberOfStateChanges += 1;
      mLastBandwidthState = bandwidthState;
    }

    public int getNumberOfStateChanges() {
      return mNumberOfStateChanges;
    }

    public ConnectionQuality getLastBandwidthState() {
      return mLastBandwidthState;
    }
  }
}
