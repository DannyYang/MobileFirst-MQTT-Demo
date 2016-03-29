/**
 * 
 */
package com.DemoMQTT;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.webcomm.util.LogUtil;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;

/**
 * @author DannyYang
 *
 */
public class DemoMQTTResources {

	public static final String TAG = DemoMQTTResources.class.getName();
	private static ConnectivityManager aConnectionManager;
	private static NotificationManager mNotifMan;
	private static DemoMQTT demoMQTT;
	private static Service service;
	private static Context ctx;
	private static String today;

	static void init(DemoMQTT demoMQTT) {
		DemoMQTTResources.demoMQTT = demoMQTT;
		loadAll(demoMQTT);
	}

	static void init(Service service) {
		DemoMQTTResources.service = service;
		loadAll(service);
	}

	private static void loadAll(Context ctx) {
		LogUtil.d(TAG, "Start MPortalResources with: ", ctx.getClass()
				.getName());
		DemoMQTTResources.ctx = ctx;
		DemoMQTTResources.aConnectionManager = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		DemoMQTTResources.mNotifMan = (NotificationManager) ctx
				.getSystemService(Context.NOTIFICATION_SERVICE);
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd", Locale.TAIWAN);
		DemoMQTTResources.today = sf.format(new Date());
	}

	public static ConnectivityManager getConnectivityManager() {
		return aConnectionManager;
	}

	public static NotificationManager getNotificationManager() {
		return mNotifMan;
	}

	public static String getToday() {
		return today;
	}

	public static boolean isService() {
		if (service == null) {
			return false;
		}
		return true;
	}

	public static DemoMQTT getDemoMQTT() {
		if (demoMQTT == null) {
			throw new RuntimeException(
					"DemoMQTTResources不是由DemoMQTT的畫面程式啓動，無法提供getDemoMQTT()的執行功能！");
		}
		return demoMQTT;
	}

	public static Service getSerivce() {
		if (service == null) {
			throw new RuntimeException(
					"DemoMQTTResources不是由Service的程式啓動，無法提供getDemoMQTT()的執行功能！");
		}
		return service;
	}

	public static Context getContext() {
		return ctx.getApplicationContext();
	}

}
