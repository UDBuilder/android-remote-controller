package org.udbuilder.remotecontroller.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.udbuilder.remotecontroller.R;
import org.udbuilder.remotecontroller.transfer.MediaDataSender;
import org.udbuilder.remotecontroller.utils.LogUtil;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class SenderActivity extends AppCompatActivity {

    private static final String TAG = "SenderActivity";

    private MediaProjectionManager mMediaProjectionMgr;
    private MediaDataSender mSender;
    private boolean mRunning = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
    }

    public void onStartShareScreen(View view) {
        mRunning = !mRunning;
        if (mRunning) {
            if (mMediaProjectionMgr == null) {
                mMediaProjectionMgr = (MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            }
            if (mMediaProjectionMgr != null) {
                startActivityForResult(mMediaProjectionMgr.createScreenCaptureIntent(), 1);
            } else {
                LogUtil.e(TAG, "onStartShareScreen mMediaProjectionMgr is null");
            }
        } else {
            if (mSender != null) {
                mSender.stop();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            LogUtil.e(TAG, "onActivityResult permission denied");
            return;
        }
        if (mSender == null) {
            MediaProjection mp = mMediaProjectionMgr.getMediaProjection(resultCode, data);
            mSender = new MediaDataSender(mp, null);
        }
        mSender.start();
    }
}
