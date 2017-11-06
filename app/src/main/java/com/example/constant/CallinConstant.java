package com.example.constant;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.example.rtstvlc.InitVideoActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * 呼叫线程
 * Created by smartLew on 2017/6/7.
 */
public class CallinConstant extends Service implements Runnable {

    private static final String TAG = "Callin";
    private Context mContext = this;

    private final static String PACKAGE = "com.example.rtstvlc";
    private final static String help_set_activity = "";

    public final static String CALL_IN = "CALLIN";//呼叫
    public final static String HUNG_UP = "HUNGUP";//挂断
    private final static int PORT = 5600;//端口
    private final static String BASE_IP = "192.168.42.125";//底座ip

    private ScreenBroadcastReceiver mScreenReceiver;//屏幕状态广播接收者


    private final static String PACKAGE_NAME = PACKAGE;


    private SaveCommandThread mThread;


    private String mCurScreenState = SCREEN_ON;//当前屏幕状态
    private final static String SCREEN_OFF = "1";//锁屏
    private final static String SCREEN_ON = "2";//开屏
    private final static String USER_PRESENT = "3";//未锁屏


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mThread = new SaveCommandThread();
        mThread.start();
        Log.e(TAG, "onCreate");

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        //  mScreenReceiver = new ScreenBroadcastReceiver();
        //startScreenBroadcastReceiver();

        Log.e(TAG, "onStart");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        // receiveCommandFromBase();//接收底座发送的指令
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    /**
     * 发送广播
     *
     * @param action
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void run() {


        while (true) {
            try {
                //1.创建一个DatagramSocket对象，并指定监听的端口号
                DatagramSocket socket = new DatagramSocket(PORT);
                //2. 创建一个byte数组用于接收
                byte buff[] = new byte[6];
                //3. 创建一个空的DatagramPackage对象
                DatagramPacket packet = new DatagramPacket(buff, buff.length);

                //4.使用receive方法接收发送方所发送的数据,同时这也是一个阻塞的方法
                socket.receive(packet);
                String result = new String(packet.getData(), packet.getOffset(), packet.getLength());

                if (!TextUtils.isEmpty(result)) {
//                    broadcastUpdate(result);
//                }

                    //有呼叫进来
                    if (CALL_IN.equals(result)) {
                        if (InitVideoActivity.mIsBackGround) {
                            startApp(PACKAGE_NAME);
                        } else {
//                                mVlcVideoActivityView.callin(result);
                            broadcastUpdate(CALL_IN);
                        }
                    }
                    //通话结束
                    else if (HUNG_UP.equals(result)) {
//                            mVlcVideoActivityView.callin(result);
                        broadcastUpdate(HUNG_UP);
                    } else {
                        continue;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //休眠500毫秒，防止程序崩溃
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 从底座接收指令
     */
    public void receiveCommandFromBase() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //1.创建一个DatagramSocket对象，并指定监听的端口号
                        DatagramSocket socket = new DatagramSocket(PORT);
                        //2. 创建一个byte数组用于接收
                        byte buff[] = new byte[6];
                        //3. 创建一个空的DatagramPackage对象
                        DatagramPacket packet = new DatagramPacket(buff, buff.length);

                        //4.使用receive方法接收发送方所发送的数据,同时这也是一个阻塞的方法
                        socket.receive(packet);
                        String result = new String(packet.getData(), packet.getOffset(), packet.getLength());

                        //有呼叫进来
                        if (CALL_IN.equals(result)) {
                            if (InitVideoActivity.mIsBackGround) {
                                startApp(PACKAGE_NAME);
                            } else {
//                                mVlcVideoActivityView.callin(result);
                                broadcastUpdate(CALL_IN);
                            }
                        }
                        //通话结束
                        else if (HUNG_UP.equals(result)) {
//                            mVlcVideoActivityView.callin(result);
                            broadcastUpdate(HUNG_UP);
                        } else {
                            continue;
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //休眠500毫秒，防止程序崩溃
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    /**
     * 检查app是否处于后台
     */
    private void checkAppIsBackground() {

//        if (InitVideoActivity.mIsBackGround) {//app处于后台
//            startApp(PACKAGE);
//        } else {//app处于前台
//            broadcastUpdate(CALL_IN);
//        }


    }


    /**
     * app后台状态下启动app
     *
     * @param appPackageName 应用包名
     */
    private void startApp(String appPackageName) {

        try {
            Intent intent = this.getPackageManager().getLaunchIntentForPackage(appPackageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Command", CALL_IN);
            startActivity(intent);
            broadcastUpdate(CALL_IN);
        } catch (Exception e) {
            Log.e(TAG, "没有安装");
        }

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


    /**
     * 注册广播
     */
    private void startScreenBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mScreenReceiver, filter);


    }

// *****************************************************************************************************************************************

    /**
     * 启动一个app
     *
     * @param com   ComponentName 对象，包含apk的包名和主Activity名
     * @param param 需要传给apk的参数
     */
    private void startApp(ComponentName com, String param) {

        if (null != com) {
            PackageInfo packageInfo;
            try {
                packageInfo = getPackageManager().getPackageInfo(com.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                packageInfo = null;
                Log.e(TAG, "没有安装");
                e.printStackTrace();
            }

            try {
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setComponent(com);
                if (null != param) {
                    Bundle bundle = new Bundle(); // 创建Bundle对象
                    bundle.putString("flag", param); // 装入数据
                    intent.putExtras(bundle); // 把Bundle塞入Intent里面
                }
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "启动异常");
            }
        }

    }

    /**
     * 接收底座发送指令的线程
     */
    private class SaveCommandThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    //1.创建一个DatagramSocket对象，并指定监听的端口号
                    DatagramSocket socket = new DatagramSocket(PORT);
                    //2. 创建一个byte数组用于接收
                    byte buff[] = new byte[6];
                    //3. 创建一个空的DatagramPackage对象
                    DatagramPacket packet = new DatagramPacket(buff, buff.length);

                    //4.使用receive方法接收发送方所发送的数据,同时这也是一个阻塞的方法
                    socket.receive(packet);
                    String result = new String(packet.getData(), packet.getOffset(), packet.getLength());

                    //有呼叫进来
                    if (CALL_IN.equals(result)) {
                        if (InitVideoActivity.mIsBackGround) {
                            startApp(PACKAGE_NAME);
                        } else {
//                                mVlcVideoActivityView.callin(result);
                            broadcastUpdate(CALL_IN);
                        }
                    }
                    //通话结束
                    else if (HUNG_UP.equals(result)) {
//                            mVlcVideoActivityView.callin(result);
                        broadcastUpdate(HUNG_UP);
                    } else {
                        continue;
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //休眠500毫秒，防止程序崩溃
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
