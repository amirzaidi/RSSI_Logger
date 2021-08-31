package com.adhocnetworks.rssi_logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

/**
 * Listens for SCAN_RESULTS_AVAILABLE_ACTION which is triggered after a completed WiFi scan.
 */
public class RSSIScanner extends BroadcastReceiver {
    private static final String TAG = "RSSIScanner";
    private static final IntentFilter FILTER = new IntentFilter();

    static {
        FILTER.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    }

    private final WifiManager mWifiManager;
    private final Runnable mOnScan;
    private boolean mScanning;

    public RSSIScanner(Context context, Runnable onScan) {
        // This is not necessary, as the scanner is destroyed together with the activity context.
        // However, Android Studio will keep its warning unless the application context is used.
        Context appContext = context.getApplicationContext();
        mWifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
        mOnScan = onScan;
    }

    public void startScanning(Context context) {
        mScanning = true;
        context.registerReceiver(this, FILTER);

        // This starts a single scan.
        mWifiManager.startScan();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ignore the result if stopScanning was already called.
        if (mScanning) {
            // Sometimes a scan fails when the WiFi connection state changes during the scan.
            boolean success = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);

            if (success) {
                mOnScan.run();
            } else {
                Log.e(TAG, "WiFi Scan Failed");
            }

            // Keep scanning as long as mScanning is true.
            mWifiManager.startScan();
        }
    }

    public void stopScanning(Context context) {
        mScanning = false;
        context.unregisterReceiver(this);
    }
}
