package com.example.rtstvlc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.base.BaseActivity;
import com.example.manager.ActivityControl;
import com.example.manager.ActivityManager;
import com.example.constant.CallinConstant;
import com.example.utils.FileWriterUtil;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class PlayRtspVideoActivity extends BaseActivity implements SurfaceHolder.Callback {

    private static final String TAG = "PlayRtspVideoActivity";
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder = null;

    private String mVideoPath = "rtsp://192.168.42.125";

    private int mVideoHeight;
    private int mVideoWidth;
    private int mSarDen;
    private int mSarNum;
    private int mUiVisibility = -1;
    private static final int SURFACE_SIZE = 3;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;


    private static final int HANDLER_VIDEO_PLAY = 1;
    private static final int HANDLER_VIDEO_STOP = 2;

    private PlayVideoReceiver mReceiver;


    private static int mPlayThreadFlag = 0;
    private static final String KEY_PLAYTHREAD_FLAG = "index";
    private static Thread mPlayingThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        ActivityManager.getInstance().addActivity(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_play_rtsp_video);

        if (savedInstanceState != null) {
            mPlayThreadFlag = savedInstanceState.getInt(KEY_PLAYTHREAD_FLAG, 0);
        }

        initView();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.i(TAG, "onSaveInstanceState");
        savedInstanceState.putInt(KEY_PLAYTHREAD_FLAG, mPlayThreadFlag);
    }
    @Override
    protected void onResume() {

        /**
         * 设置为横屏
         */
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }


        super.onResume();
        // 声明广播
        mReceiver = new PlayVideoReceiver();
        //注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallinConstant.CALL_IN);
        intentFilter.addAction(CallinConstant.HUNG_UP);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mReceiver) {
            unregisterReceiver(mReceiver);
        }
    }

    private void startPlaying()
    {
        if (mPlayingThread == null || !mPlayingThread.isAlive()) {
            mPlayingThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        mPlayThreadFlag = 1;
                        //setWindowBrightness(255);
                        VideoPlayer.play(mVideoPath, mSurfaceHolder.getSurface());

                        finish();
                        mPlayThreadFlag = 0;
                    }
                });
            mPlayingThread.start();
        } else {
            Log.e(TAG, "PlayingThread is still running.");


        }
    }

    private void initView() {

        mSurfaceView = ((SurfaceView) findViewById(R.id.rtsp_surfaceview));

        ((Button) findViewById(R.id.btn_back)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                ActivityManager.getInstance().finshActivities(PlayRtspVideoActivity.class);
            }
        });
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        startPlaying();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        VideoPlayer.stop();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    //重新播放视频handler
    private Handler mVideoPlayHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_VIDEO_PLAY://呼叫进来
                    mSurfaceHolder = mSurfaceView.getHolder();
                    //mSurfaceHolder.setFormat(PixelFormat.RGBX_8888);
                    mSurfaceHolder.addCallback(PlayRtspVideoActivity.this);
                    Toast.makeText(PlayRtspVideoActivity.this, "开始通话", Toast.LENGTH_SHORT).show();

                    break;
                case HANDLER_VIDEO_STOP://通话结束
                    Toast.makeText(PlayRtspVideoActivity.this, "通话结束", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
            }
        }
    };

    /**
     * Convert time to a string
     *
     * @param millis e.g.time/length from file
     * @return formated string (hh:)mm:ss
     */
    public static String millisToString(long millis) {
        boolean negative = millis < 0;
        millis = java.lang.Math.abs(millis);

        millis /= 1000;
        int sec = (int) (millis % 60);
        millis /= 60;
        int min = (int) (millis % 60);
        millis /= 60;
        int hours = (int) millis;

        String time;
        DecimalFormat format = (DecimalFormat) NumberFormat
                .getInstance(Locale.US);
        format.applyPattern("00");
        if (millis > 0) {
            time = (negative ? "-" : "") + hours + ":" + format.format(min)
                    + ":" + format.format(sec);
        } else {
            time = (negative ? "-" : "") + min + ":" + format.format(sec);
        }
        return time;
    }


    private void changeSurfaceSize() {
        // get screen size
        int dw = getWindow().getDecorView().getWidth();
        int dh = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (dw > dh && isPortrait || dw < dh && !isPortrait) {
            int d = dw;
            dw = dh;
            dh = d;
        }
        if (dw * dh == 0)
            return;
        // compute the aspect ratio
        double ar, vw;
        double density = (double) mSarNum / (double) mSarDen;
        if (density == 1.0) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoWidth;
            ar = (double) mVideoWidth / (double) mVideoHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoWidth * density;
            ar = vw / mVideoHeight;
        }

        // compute the display aspect ratio
        double dar = (double) dw / (double) dh;

        // // calculate aspect ratio
        // double ar = (double) mVideoWidth / (double) mVideoHeight;
        // // calculate display aspect ratio
        // double dar = (double) dw / (double) dh;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                // mTextShowInfo.setText(R.string.video_player_best_fit);
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_FIT_HORIZONTAL:
                // mTextShowInfo.setText(R.string.video_player_fit_horizontal);
                dh = (int) (dw / ar);
                break;
            case SURFACE_FIT_VERTICAL:
                // mTextShowInfo.setText(R.string.video_player_fit_vertical);
                dw = (int) (dh * ar);
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                //  mTextShowInfo.setText(R.string.video_player_16x9);
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_4_3:
                //  mTextShowInfo.setText(R.string.video_player_4x3);
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_ORIGINAL:
                // mTextShowInfo.setText(R.string.video_player_original);
                dh = mVideoHeight;
                dw = mVideoWidth;
                break;
        }

        mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = dw;
        lp.height = dh;
        mSurfaceView.setLayoutParams(lp);
        mSurfaceView.invalidate();
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                Toast.makeText(PlayRtspVideoActivity.this, "请等待视频自动关闭", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }


    @Override
    public void onBackPressed() {
        Toast.makeText(PlayRtspVideoActivity.this, "请等待视频自动关闭", Toast.LENGTH_SHORT).show();
    }


    /**
     * 播放视频广播接收器
     */
    private class PlayVideoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (CallinConstant.CALL_IN.equals(action)) {

                mVideoPlayHandler.sendEmptyMessage(HANDLER_VIDEO_PLAY);
            } else if (CallinConstant.HUNG_UP.equals(action)) {


                mVideoPlayHandler.sendEmptyMessage(HANDLER_VIDEO_STOP);
            }

        }
    }

    private void setWindowBrightness(int brightness) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness / 255.0f;
        window.setAttributes(lp);
    }
}
