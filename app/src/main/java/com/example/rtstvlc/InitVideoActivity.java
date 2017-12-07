package com.example.rtstvlc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.base.BaseActivity;
import com.example.constant.CallinConstant;
import com.example.manager.ActivityControl;
import com.example.manager.ActivityManager;
import com.example.service.DaemonService;
import com.example.service.HansIndoorService;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InitVideoActivity extends BaseActivity {
    protected static final String TAG = "InitVideoActivity";
    private Button btn_play;
    private PlayVideoReceiver mReceiver;

    public static boolean mIsBackGround = false;

    private final static int HANDER_PLAY_VIDEO = 1;
    private final static int HANDER_STOP_VIDEO = 2;
    private final static int HANDER_SHOW_TIME = 3;

    //private UDPClient udpClient;
    private void setWindowBrightness(int brightness) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness / 255.0f;
        window.setAttributes(lp);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDER_PLAY_VIDEO:
                    Toast.makeText(InitVideoActivity.this, "开始播放", Toast.LENGTH_SHORT).show();
                    //playVideo();
                    setWindowBrightness(255);
                    startActivity(new Intent(InitVideoActivity.this, PlayRtspVideoActivity.class));
                    break;
                case HANDER_STOP_VIDEO:
                    Toast.makeText(InitVideoActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
                    break;
                case HANDER_SHOW_TIME://实时改变时间
                    long sysTime = System.currentTimeMillis();
                    CharSequence sysTimeStr = DateFormat.format("HH:mm:ss", sysTime);
                    tv_time.setText(sysTimeStr); //更新时间
                    if ("00:00:01".equals(sysTimeStr)) {
                        Date date = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        tv_date.setText(sdf.format(date) + "");
                    }

                    break;
                default:
                    break;
            }
        }
    };
    private TextView tv_date;
    private TextView tv_time;


    //  public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;

    /**
     * 播放视频广播接收器
     */
    private class PlayVideoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (CallinConstant.CALL_IN.equals(action)) {
                mHandler.sendEmptyMessage(HANDER_PLAY_VIDEO);

            } else if (CallinConstant.HUNG_UP.equals(action)) {

                mHandler.sendEmptyMessage(HANDER_STOP_VIDEO);
            }

        }
    }

    public void RequestPermissionOfWriteSettings(){
        Log.e(TAG, "Enable USB tethering !");

        if (!Settings.System.canWrite(this)) {
            Log.e(TAG, "request System.canWrite.");
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Log.e(TAG, "request Permission WriteSettings successfully.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏

        // this.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED, FLAG_HOMEKEY_DISPATCHED);//关键代码
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_init_video);

        // Log.e(TAG, "-----------onCreate-----------");

        mIsBackGround = false;

        ActivityManager.getInstance().addActivity(this);

        //声明广播
        mReceiver = new PlayVideoReceiver();
        //注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallinConstant.CALL_IN);
        intentFilter.addAction(CallinConstant.HUNG_UP);
        getApplicationContext().registerReceiver(mReceiver, intentFilter);

        btn_play = ((Button) findViewById(R.id.btn_play));
        tv_date = ((TextView) findViewById(R.id.tv_date));
        tv_time = ((TextView) findViewById(R.id.tv_time));

        RequestPermissionOfWriteSettings();

        startService(new Intent(this, HansIndoorService.class));
        startService(new Intent(this, DaemonService.class));

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        tv_date.setText(sdf.format(date) + "");

        new Thread(new TimeThread()).start();//开启实时更新时间线程


        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //playVideo();
                Log.e(TAG, "[setOnClickListener]start play video !!! ");
                startActivity(new Intent(InitVideoActivity.this, PlayRtspVideoActivity.class));
            }
        });
        setWindowBrightness(0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsBackGround = false;


        /**
         * 设置为横屏
         */
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }


        Log.e("屏幕方向", getRequestedOrientation() + "");


        Intent intent = getIntent();
        String commond = intent.getStringExtra(CallinConstant.CALL_IN);
        if (!TextUtils.isEmpty(commond) && TextUtils.equals(CallinConstant.CALL_IN, commond)) {
            //playVideo();
            Log.e(TAG, "[onResume]start play video !!! ");
            startActivity(new Intent(InitVideoActivity.this, PlayRtspVideoActivity.class));
        } else {
            setWindowBrightness(0);
        }


    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //  Log.e(TAG, "-----------onStop-----------");
        // mIsBackGround = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (null != mReceiver) {
//            getApplicationContext().unregisterReceiver(mReceiver);
//        }

        // udpClient.setUdpLife(false);
        //  Log.e(TAG, "-----------onPause-----------");
        //  mIsBackGround = true;

    }

    @Override
    protected void onStart() {
        mIsBackGround = false;
        super.onStart();
        //  Log.e(TAG, "-----------onStart-----------");
    }


    @Override
    protected void onDestroy() {
        mIsBackGround = true;

//        Log.e(TAG, "-----------onDestroy-----------");
        //EventHandler em = EventHandler.getInstance();
        //em.removeHandler(handler);
        // System.exit(0);
        super.onDestroy();
    }

//    @Override
//    public void onBackPressed() {
//        mIsBackGround = true;
//        super.onBackPressed();
//        // stopService(new Intent(this, HansIndoorService.class));
//    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                mIsBackGround = true;
                return true;
            }
//        } else if (event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
//            return false;
        }
        return super.dispatchKeyEvent(event);
    }


    @Override
    public void onBackPressed() {
        mIsBackGround = true;

//        String a = "dff";
//        int b;
//
//        b = Integer.parseInt(a);
//        tv_date.setText(b + "");

        super.onBackPressed();
    }


    private class TimeThread extends Thread {

        @Override
        public void run() {
            do {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = HANDER_SHOW_TIME;  //消息(一个整型值)
                    mHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }
}
