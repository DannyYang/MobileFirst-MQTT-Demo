package com.DemoMQTT;

import com.webcomm.mqtt.MQProxy;
import com.webcomm.mqtt.MsgArrivedCallback;
import com.webcomm.util.LogUtil;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

/**
 * 
 * 
 * @author DannyYang
 * 
 */
public class StandAlongMQService extends Service {

	public static final String TAG = StandAlongMQService.class.getName();
	// Notification title
	public static String NOTIF_TITLE = "dannyTest";

	private MsgArrivedCallback callback;
	private MQProxy proxy;

	private HandlerThread worker;
	private Handler task;

	@Override
	public void onCreate() {
		super.onCreate();
		LogUtil.d(TAG, "StandAlongMQService onCreate.");
		DemoMQTTResources.init(this);
		this.callback = new MsgCallback();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {

		super.onStart(intent, startId);
		LogUtil.d(TAG, "StandAlongMQService onStart.");

		worker = new HandlerThread(TAG);
		worker.start();
		task = new Handler(worker.getLooper());

		try {
			SharedPreferences sp = getSharedPreferences(
					"PersistenceSharedPreferences", Context.MODE_PRIVATE);
			String message = sp.getString("message", "");
			String clientId = sp.getString("clientId", "");
			String topic = sp.getString("topic", "");

			if (clientId != null && topic != null) {
				connnectMQ(clientId, topic);
			} else {
				LogUtil.d(TAG, "clientId or topic is null, mqtt not connect");
				stopSelf();
			}

		} catch (Exception e) {
			LogUtil.e(TAG, e, "Start Service Error! MSG :", e.getMessage());
			stopSelf();
		}
	}

	@Override
	public void onDestroy() {
		LogUtil.d(TAG, "StandAlongMQService onDestroy.");

		if (proxy != null) {
			proxy.disconnect();
		}

		super.onDestroy();
	}

	/**
	 * 連接MQ
	 * 
	 * @param clientId
	 * @param uid
	 */
	private void connnectMQ(final String clientId, final String topic) {
		task.post(new Runnable() {
			@Override
			public void run() {
				proxy = MQProxy.getInstance();
				proxy.connect(clientId, topic, callback);
			}
		});
	}

	private class MsgCallback extends MsgArrivedCallback {

		@Override
		public void messageArrived(String msg) {
			if (msg != null) {
				showNotification(msg);
				// mportal.callMessageHandlerRouters(msg.appId, msg.nativeType,
				// msg.body);
			} else {
				LogUtil.e(TAG, "Message ERROR! ");
			}
		}
	}

	/**
	 * 在手機上方顯示推播訊息
	 * 
	 * @param text
	 */
	private void showNotification(String msg) {
		LogUtil.d(TAG, "StandAlongMQService showNotification.", msg.toString());

		Notification n = new Notification();

		n.flags |= Notification.FLAG_SHOW_LIGHTS;
		n.flags |= Notification.FLAG_AUTO_CANCEL;

		n.defaults = Notification.DEFAULT_ALL;
		n.defaults ^= Notification.DEFAULT_SOUND;

		n.icon = R.drawable.icon;
		n.when = System.currentTimeMillis();

		// Simply open the parent activity
		Intent i = this.makeMQTTIntent(msg);

		PendingIntent pi = PendingIntent.getActivity(DemoMQTTResources
				.getContext().getApplicationContext(), 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT);

		LogUtil.d(TAG, "Parser mqtt message to Notification! ");
		n.setLatestEventInfo(DemoMQTTResources.getContext(), NOTIF_TITLE, msg,
				pi);

		// 只保留最新的 Notification 訊息
		DemoMQTTResources.getNotificationManager().notify(0, n);
	}

	/**
	 * 依 MQTT 訊息產生傳遞給 Activity 的 Intent
	 * 
	 * @param appId
	 * @param type
	 * @param msg
	 * @return
	 */
	private Intent makeMQTTIntent(String msg) {
		Intent i = new Intent(DemoMQTTResources.getContext()
				.getApplicationContext(), DemoMQTT.class);

		// KenTsai 這邊改成寫到檔案去
		// 傳入 MQTT 訊息
		// i.putExtra("appId", appId);
		// i.putExtra("type", type);
		// i.putExtra("msg", msg);

		// 加上 FLAG_ACTIVITY_SINGLE_TOP 才能夠呼叫 Activity 的 onNewItent 傳入 Intent
		// 加上 FLAG_ACTIVITY_NEW_TASK 才能使用 startActivity() 啟動 Activity
		i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);

		return i;
	}

}
