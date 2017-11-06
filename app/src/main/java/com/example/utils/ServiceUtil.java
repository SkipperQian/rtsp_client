package com.example.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import java.util.List;

/**
 * Created by smartLew on 2017/8/2.
 */

public class ServiceUtil {

    public static boolean isServiceAlive(Context context, String serviceName) {

        ActivityManager manager = ((ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningServiceInfo> running = manager.getRunningServices(30);
        for (int i = 0; i < running.size(); i++) {
            if (serviceName.equals(running.get(i).service.getClassName())) {
                return true;
            }
        }
        return false;

    }

    public static boolean isApplicationBroughtToBackground(Context context) {

        ActivityManager manager = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningTaskInfo> tasks = manager.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }


        return false;

    }

}
