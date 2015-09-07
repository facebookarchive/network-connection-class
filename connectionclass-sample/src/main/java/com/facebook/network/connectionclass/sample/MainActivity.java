/*
 *  This file provided by Facebook is for non-commercial testing and evaluation
 *  purposes only.  Facebook reserves all rights not expressly granted.
 *
 *  This source code is licensed under the BSD-style license found in the
 *  LICENSE file in the root directory of this source tree. An additional grant
 *  of patent rights can be found in the PATENTS file in the same directory.
 *
 */

package com.facebook.network.connectionclass.sample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.facebook.network.connectionclass.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends Activity {

  private static final String TAG = "ConnectionClass-Sample";

  private ConnectionClassManager mConnectionClassManager;
  private DeviceBandwidthSampler mDeviceBandwidthSampler;
  private ConnectionChangedListener mListener;
  private TextView mTextView;
  private View mRunningBar;

  private String mURL = "http://connectionclass.parseapp.com/m100_hubble_4060.jpg";
  private int mTries = 0;
  private ConnectionQuality mConnectionClass = ConnectionQuality.UNKNOWN;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mConnectionClassManager = ConnectionClassManager.getInstance();
    mDeviceBandwidthSampler = DeviceBandwidthSampler.getInstance();
    findViewById(R.id.test_btn).setOnClickListener(testButtonClicked);
    mTextView = (TextView)findViewById(R.id.connection_class);
    mTextView.setText(mConnectionClassManager.getCurrentBandwidthQuality().toString());
    mRunningBar = findViewById(R.id.runningBar);
    mRunningBar.setVisibility(View.GONE);
    mListener = new ConnectionChangedListener();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mConnectionClassManager.remove(mListener);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mConnectionClassManager.register(mListener);
  }

  /**
   * Listener to update the UI upon connectionclass change.
   */
  private class ConnectionChangedListener
      implements ConnectionClassManager.ConnectionClassStateChangeListener {

    @Override
    public void onBandwidthStateChange(ConnectionQuality bandwidthState) {
      mConnectionClass = bandwidthState;
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          mTextView.setText(mConnectionClass.toString());
        }
      });
    }
  }

  private final View.OnClickListener testButtonClicked = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        new DownloadImage().execute(mURL);
    }
  };

  /**
   * AsyncTask for handling downloading and making calls to the timer.
   */
  private class DownloadImage extends AsyncTask<String, Void, Void> {

    @Override
    protected void onPreExecute() {
      mDeviceBandwidthSampler.startSampling();
      mRunningBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(String... url) {
      String imageURL = url[0];
      try {
        // Open a stream to download the image from our URL.
        URLConnection connection = new URL(imageURL).openConnection();
        connection.setUseCaches(false);
        connection.connect();
        InputStream input = connection.getInputStream();
        try {
          byte[] buffer = new byte[1024];

          // Do some busy waiting while the stream is open.
          while (input.read(buffer) != -1) {
          }
        } finally {
          input.close();
        }
      } catch (IOException e) {
        Log.e(TAG, "Error while downloading image.");
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void v) {
      mDeviceBandwidthSampler.stopSampling();
      // Retry for up to 10 times until we find a ConnectionClass.
      if (mConnectionClass == ConnectionQuality.UNKNOWN && mTries < 10) {
        mTries++;
        new DownloadImage().execute(mURL);
      }
      if (!mDeviceBandwidthSampler.isSampling()) {
        mRunningBar.setVisibility(View.GONE);
      }
    }
  }
}
