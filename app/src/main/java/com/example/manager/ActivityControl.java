package com.example.manager;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

public class ActivityControl {
	private static List<Activity> acitivityList = new ArrayList<Activity>();

	public static void addActivity(Activity activity) {
		acitivityList.add(activity);
	}

	public static void removeActivity(Activity activity) {
		acitivityList.remove(activity);
	}

	public static void finishProgrom() {
		for (Activity activity : acitivityList) {
			if (null != activity) {
				activity.finish();
			}
		}
		android.os.Process.killProcess(android.os.Process.myPid());
	}

}
