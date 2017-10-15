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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
public class ConnectionClassTest {

  @Mock
  public ConnectionClassManager mConnectionClassManager;
  public TestBandwidthStateChangeListener mTestBandwidthStateChangeListener;

  private static final long BYTES_TO_BITS = 8;

  @Before
  public void setUp() {
    mConnectionClassManager = ConnectionClassManager.getInstance();
    mTestBandwidthStateChangeListener = new TestBandwidthStateChangeListener();
    mConnectionClassManager.reset();
  }

  //Test the moving average to make sure correct results are returned.
  @Test
  public void TestMovingAverage() {
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

  @Test
  public void testStateChangeHysteresisRejectsLow() {
    runHysteresisTest(
            ConnectionClassManager.DEFAULT_POOR_BANDWIDTH,
            1.02,
            ConnectionQuality.MODERATE,
            (100.0 - ConnectionClassManager.DEFAULT_HYSTERESIS_PERCENT / 2) / 100.0,
            ConnectionQuality.MODERATE);
  }

  @Test
  public void testStateChangeHysteresisRejectsHigh() {
    runHysteresisTest(
            ConnectionClassManager.DEFAULT_MODERATE_BANDWIDTH,
            .98,
            ConnectionQuality.MODERATE,
            100.0 / (100.0 - ConnectionClassManager.DEFAULT_HYSTERESIS_PERCENT / 2),
            ConnectionQuality.MODERATE);
  }

  @Test
  public void testStateChangeHysteresisAcceptsLow() {
    runHysteresisTest(
            ConnectionClassManager.DEFAULT_POOR_BANDWIDTH,
            1.02,
            ConnectionQuality.MODERATE,
            (100.0 - ConnectionClassManager.DEFAULT_HYSTERESIS_PERCENT * 2) / 100.0,
            ConnectionQuality.POOR);
  }

  @Test
  public void testStateChangeHysteresisAcceptsHigh() {
    runHysteresisTest(
            ConnectionClassManager.DEFAULT_MODERATE_BANDWIDTH,
            0.98,
            ConnectionQuality.MODERATE,
            100.0 / (100.0 - ConnectionClassManager.DEFAULT_HYSTERESIS_PERCENT * 2),
            ConnectionQuality.GOOD);
  }

  private void runHysteresisTest(
          double bandwidthBoundary,
          double initialMultiplier,
          ConnectionQuality initialQuality,
          double finalMultiplier,
          ConnectionQuality finalQuality) {
    int milliseconds = 5;

    // Run just enough samples to set the initial quality.
    for (int i = 0; i < ConnectionClassManager.DEFAULT_SAMPLES_TO_QUALITY_CHANGE + 1; i++) {
      long barelyModerateBytes = bytesPerUpdate(bandwidthBoundary, initialMultiplier, milliseconds);
        mConnectionClassManager.addBandwidth(barelyModerateBytes, milliseconds);
    }
    assertEquals(initialQuality, mTestBandwidthStateChangeListener.getLastBandwidthState());

    // Run enough samples at the new rate that the moving average should now be close to this rate.
    for (int i = 0; i < 2 * ConnectionClassManager.DEFAULT_SAMPLES_TO_QUALITY_CHANGE; i++) {
      long quitePoorBytes = bytesPerUpdate(bandwidthBoundary, finalMultiplier, milliseconds);;
        mConnectionClassManager.addBandwidth(quitePoorBytes, milliseconds);
    }
    assertEquals(finalQuality, mTestBandwidthStateChangeListener.getLastBandwidthState());
  }

  static private long bytesPerUpdate(
          double bandwidthBoundary,
          double multiplier,
          long milliseconds) {
    double bytes = bandwidthBoundary * multiplier * milliseconds / BYTES_TO_BITS;
    bytes = multiplier > 1.0 ? Math.ceil(bytes) : Math.floor(bytes);
    return (long) bytes;
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
