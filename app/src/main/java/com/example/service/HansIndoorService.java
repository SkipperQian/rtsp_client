package com.example.service;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.example.constant.CallinConstant;
import com.example.manager.ActivityControl;
import com.example.manager.ActivityManager;
import com.example.rtstvlc.InitVideoActivity;
import com.example.rtstvlc.PlayRtspVideoActivity;
import com.example.utils.ServiceUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by smartLew on 2017/6/13.
 */
public class HansIndoorService extends HansIntentService {

    private final static String TAG = "【HansIndoorService】";

    private final static String SCREEN_OFF = "1";//锁屏
    private final static String SCREEN_ON = "2";//开屏
    private final static String USER_PRESENT = "3";//未锁屏
    private String mCurScreenState = SCREEN_ON;//当前屏幕状态

    private BroadcastReceiver mTetherChangeReceiver;
    private boolean mUsbConnected;

    private final static String PACKAGE_NAME = "com.example.rtstvlc";

    private static DatagramSocket socket = null;
    private static DatagramPacket packetSend, packetRcv;
    private boolean udpLife = true; //udp生命线程
    private byte[] msgRcv = new byte[1024]; //接收消息
    final static int udpPort = 5600;

    private static Thread mThread;

    private Context mContext;
    private ScreenBroadcastReceiver mScreenReceiver;

    public HansIndoorService(String name) {
        super(name);
    }

    public HansIndoorService() {
        super("");
    }


    @Override
    public void onCreate() {
        super.onCreate();

    }

    private void setUsbTethering(boolean enabled) throws ClassNotFoundException {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        Method m;

        Class c = Class.forName("android.net.ConnectivityManager");
        try {
            m = c.getMethod("setUsbTethering", new Class[]{boolean.class});
            m.invoke(cm, enabled);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "setUsbTethering successfully!");
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "TetherChangeReceiver: onReceive");
            if (action.equals("android.hardware.usb.action.USB_STATE")) {
                mUsbConnected = intent.getBooleanExtra("connected", false);
                Log.i(TAG, "TetherChangeReceiver: android.hardware.usb.action.USB_STATE" + " mUsbConnected = " + mUsbConnected);

                if (mUsbConnected == true) {
                    try {
                        setUsbTethering(mUsbConnected);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mScreenReceiver = new ScreenBroadcastReceiver();
        startScreenBroadcastReceiver();

        //RequestPermissionOfWriteSettings();



        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        getApplicationContext().registerReceiver(mTetherChangeReceiver, filter);
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startDaemon();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startDaemon() {

        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                if (!ServiceUtil.isServiceAlive(HansIndoorService.this, "com.example.service.DaemonService")) {
                                    Log.e("SERVICE2", "检测到服务DaemonService不存在......");
                                    startService(new Intent("com.example.rtstvlc.service.DaemonService"));
                                }
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
            mThread.start();
        }

    }

    @Override
    protected void onHandlerIntent(Intent intent) {

        try {
            socket = new DatagramSocket(udpPort);
            socket.setSoTimeout(5000);//设置超时未5s
        } catch (SocketException e) {
            e.printStackTrace();
        }
        packetRcv = new DatagramPacket(msgRcv, msgRcv.length);
        while (udpLife) {
            try {
                if (socket == null) {
                    socket = new DatagramSocket(udpPort);
                    socket.setSoTimeout(5000);//设置超时未5s
                    packetRcv = new DatagramPacket(msgRcv, msgRcv.length);
                }
                socket.receive(packetRcv);
                String RcvMsg = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength());
                if (!TextUtils.isEmpty(RcvMsg)) {

                    //if (mCurScreenState.equals(SCREEN_ON) || mCurScreenState.equals(SCREEN_OFF)) {
                    //    wakeAndUnlock(true);
                    //}

                    //先注释掉，如果接收不到广播就启用这段代码直接关闭playRtsp界面
//                    if (RcvMsg.equals(CallinConstant.HUNG_UP)) {
//                        ActivityManager.getInstance().finshActivities(PlayRtspVideoActivity.class);//强制关闭PlayRtsp界面
//                    }

                    updateBroadcast(RcvMsg);


                    Log.e(TAG, "recv: " + RcvMsg);

//                    if (InitVideoActivity.mIsBackGround) {
//                        //  updateBroadcast(RcvMsg);
//                        startApp(PACKAGE_NAME, RcvMsg);
//                    } else {
//                        updateBroadcast(RcvMsg);
//                    }


                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();

    }


    /**
     * app后台状态下启动app
     *
     * @param appPackageName 应用包名
     */
    private void startApp(String appPackageName, String action) {

        try {
            Intent intent = this.getPackageManager().getLaunchIntentForPackage(appPackageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(CallinConstant.CALL_IN, CallinConstant.CALL_IN);
            startActivity(intent);
            // updateBroadcast(action);
        } catch (Exception e) {
            Log.e(TAG, "没有安装");
        }

    }

    /**
     * 发送广播
     *
     * @param action
     */
    public void updateBroadcast(String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);

    }


    /**
     * 注册广播
     */
    private void startScreenBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver, filter);


    }

    /**
     * 接收系统发送的屏幕状态广播
     */
    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        private String action = null;

        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                // 开屏
                Log.e(TAG, "锁屏状态：开屏");
                mCurScreenState = SCREEN_ON;
                wakeAndUnlock(true);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // 锁屏
                Log.e(TAG, "锁屏状态：锁屏");
                mCurScreenState = SCREEN_OFF;
                wakeAndUnlock(true);
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {//用户界面
                // 解锁
                Log.e(TAG, "锁屏状态：解锁");//
                mCurScreenState = USER_PRESENT;
            }
        }

    }


    /**
     * 解锁屏幕
     *
     * @param lock true：解锁  false 锁屏
     */
    private void wakeAndUnlock(boolean lock) {

//        //屏幕解锁
//        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
//        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock(LOCK_TAG);
//        keyguardLock.disableKeyguard();

        //获取电源管理器对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        KeyguardManager.KeyguardLock kl;

        //锁屏状态
        if (lock) {
            //点亮屏幕
            wl.acquire();
            //得到键盘锁管理器对象
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            kl = km.newKeyguardLock("unLock");
            //解锁
            kl.disableKeyguard();
            Intent intent = new Intent(this, InitVideoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            //checkAppIsBackground();
        }
        //未锁屏状态
        else {
            //得到键盘锁管理器对象
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            kl = km.newKeyguardLock("lock");
            //锁屏
            kl.reenableKeyguard();
            //释放wakeLock，关灯
            wl.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getApplicationContext().unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
    }
}
