package com.android.systemui.statusbar.powerwidget;

import com.android.systemui.R;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;

public class MediaScanButton extends PowerButton {
    private static final String TAG = "MediaScanButton";
	private MediaScannerConnection mMsc;
    private boolean mIsScanning = false;

    public MediaScanButton() { mType = BUTTON_MEDIA_SCAN; }

    @Override
    protected void setupButton(View view) {
        super.setupButton(view);

        if(mView != null) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, action);
        if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
            mIsScanning = true;
            updateState(null);
        } else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
            mIsScanning = false;
            updateState(null);
        }
    }

    @Override
    protected IntentFilter getBroadcastIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addDataScheme("file");
        return filter;
    }

    @Override
    protected void updateState(Context context) {
        if (mIsScanning) {
            mIcon = R.drawable.stat_media_scan_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.stat_media_scan_off;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState(Context context) {
        // If ON turn OFF else turn ON
        if (!mIsScanning) {
            Context ctx = mView.getContext();
            ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
                    Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }

    @Override
    protected boolean handleLongClick(Context context) {
        return true;
    }
}
