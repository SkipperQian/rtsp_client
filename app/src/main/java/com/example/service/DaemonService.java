package com.example.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.utils.ServiceUtil;

/**
 * Created by smartLew on 2017/8/4.
 */
public class DaemonService extends Service {


    private static Thread mThread;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startDaemon();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startDaemon() {

        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(
                    new Runnable() {    //D:\workspace\asWorkspace\RTSTVLC\app\src\main\java\com\example\service\DaemonService.java
                        @Override
                        public void run() {
                            while (true) {
                                if (!ServiceUtil.isServiceAlive(DaemonService.this, "com.example.service.HansIndoorService")) {
                                    Log.e("SERVICE2", "检测到服务HansIndoorService不存在......");
                                    startService(new Intent("com.example.rtstvlc.service.HansIndoorService"));
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
}