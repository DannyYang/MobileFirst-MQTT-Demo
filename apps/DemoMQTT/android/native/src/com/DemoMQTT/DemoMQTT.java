package com.DemoMQTT;

import org.apache.commons.lang.StringUtils;
import org.apache.cordova.CordovaActivity;
import com.DemoMQTT.MQService.MQBinder;
import com.webcomm.util.LogUtil;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import com.worklight.androidgap.api.WL;
import com.worklight.androidgap.api.WLInitWebFrameworkResult;
import com.worklight.androidgap.api.WLInitWebFrameworkListener;

public class DemoMQTT extends CordovaActivity implements
		WLInitWebFrameworkListener {
	private MQServiceConnection conn = new MQServiceConnection();
	private MQConnectTask connectMQ = new MQConnectTask();
	private MQService mqs;
	private boolean mqsConnected = false;
	private String clientId;
	private String topic;
	private HandlerThread worker;
	private Handler task;
	private boolean isResume = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WL.createInstance(this);

		WL.getInstance().showSplashScreen(this);

		WL.getInstance().initializeWebFramework(getApplicationContext(), this);

		loadWorker();
		loadResource();

		// 從未啟動狀態啟動時，不理會 Intent 訊息(MQTT訊息)
		this.setIntent(new Intent());
	}

	/**
	 * The IBM Worklight web framework calls this method after its
	 * initialization is complete and web resources are ready to be used.
	 */
	public void onInitWebFrameworkComplete(WLInitWebFrameworkResult result) {
		if (result.getStatusCode() == WLInitWebFrameworkResult.SUCCESS) {
			super.loadUrl(WL.getInstance().getMainHtmlFilePath());
		} else {
			handleWebFrameworkInitFailure(result);
		}
	}

	private void handleWebFrameworkInitFailure(WLInitWebFrameworkResult result) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setNegativeButton(R.string.close,
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});

		alertDialogBuilder.setTitle(R.string.error);
		alertDialogBuilder.setMessage(result.getMessage());
		alertDialogBuilder.setCancelable(false).create().show();
	}

	/**
	 * 在onResume的過程中透過Thread必須關閉{@link StandAlongMQService}同時啓動{@link MQService}
	 * ． 做在onResume是因為從背景轉前景及啟動程式都會執行onResume．
	 * 
	 * @see com.worklight.androidgap.WLDroidGap#onResume()
	 */
	public void onResume() {
		super.onResume();
		LogUtil.d(TAG, "Activity onResume");

		SharedPreferences sp = this.getSharedPreferences(
				"PersistenceSharedPreferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor spEditor = sp.edit();

		spEditor.putString("ISFOREGROUND", "TRUE").commit();

		// 關閉StandAlongMQService準備切換Service．先不處理專心做畫面
		if (isResume != true) {
			isResume = true;

			task.post(new Runnable() {
				@Override
				public void run() {
					if (DemoMQTT.this
							.stopService(new Intent(getApplicationContext(),
									StandAlongMQService.class)) == false) {
						LogUtil.d(TAG,
								"Activity onResume stop standAlong fail --------------------------");
					} else {
						LogUtil.d(TAG,
								"Activity onResume stop standAlong OK --------------------------");
					}
				}
			});
			// 啟動 MQService
			task.post(new Runnable() {

				@Override
				public void run() {
					DemoMQTT.this.bindService(new Intent(DemoMQTT.this,
							MQService.class), conn, Context.BIND_AUTO_CREATE);
				}
			});
		}

		// 如果是 Notification Intent 就呼叫 JavaScript
		task.post(new Runnable() {

			/**
			 * 將 Intent 內容傳進 JavaScript MessageHandlerRouter()
			 */
			@Override
			public void run() {
				NotificationManager mNotifMan = DemoMQTTResources
						.getNotificationManager();

				if (mNotifMan != null) {
					mNotifMan.cancel(0);
				}

				LogUtil.d(TAG, "User has logined... call js to exec handler...");
				try {
					SharedPreferences sp = DemoMQTT.this.getSharedPreferences(
							"PersistenceSharedPreferences",
							Context.MODE_PRIVATE);
					String message = sp.getString("message", "");

					if (StringUtils.isNotBlank(message)) {
						DemoMQTT.this.callMessageHandlerRouters(message);
					}
					SharedPreferences.Editor spEditor = sp.edit();

					spEditor.remove("message").commit();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		LogUtil.d(TAG, "Activity onResume finish............................");
	}

	/**
	 * onPause代表程式將離開前景，可能是關閉OR背景修眠，所以必須切換{@link MQService}成
	 * {@link StandAlongMQService}．
	 * 
	 * @see com.worklight.androidgap.WLDroidGap#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();
		LogUtil.d(TAG, "Activity onPause");

		SharedPreferences sp = this.getSharedPreferences(
				"PersistenceSharedPreferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor spEditor = sp.edit();

		spEditor.putString("ISFOREGROUND", "FALSE").commit();

		isResume = false;
		// 如果MQService已經啟動了必須把它停止掉．
		if (mqsConnected) {
			task.post(new Runnable() {
				@Override
				public void run() {
					DemoMQTT.this.unbindService(conn);
				}
			});
		}
		// 啓動StandAlongMQService繼續接收ＭＱ訊息．先不處理專心作畫面
		task.post(new Runnable() {
			@Override
			public void run() {
				DemoMQTT.this.startService(new Intent(DemoMQTT.this,
						StandAlongMQService.class));
			}
		});

		LogUtil.d(TAG, "Activity onPause finish............................");
	}

	/**
	 * 儲存從 Notification 發送的 Intent
	 */
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		LogUtil.d(TAG, "Activity onNewIntent");
		this.setIntent(intent);
	}

	/**
	 * 啓動系統需要的背後執行緒.
	 */
	private void loadWorker() {
		worker = new HandlerThread(TAG);
		worker.start();
		task = new Handler(worker.getLooper());
	}

	/**
	 * 透過HandlerThread載入系統參數的XML設定檔.
	 */
	private void loadResource() {
		// KenTsai 這邊的task做法要拿掉,不然會有機會造成onResume發生的時候找不到檔案,因為task有時候在某些手持裝置
		// 資源較差的情形下會跑到後面才執行,這樣就會導致無法將前背景狀態的資訊寫到檔案,導致MQTT不做連線的情形
		// task.post(new Runnable() {

		// @Override
		// public void run() {
		DemoMQTTResources.init(DemoMQTT.this);
		// }
		// });
	}

	/**
	 * MPORTAL內部介接{@link MQService}的{@link ServiceConnection}.
	 * 
	 * @author baytony
	 * 
	 */
	private class MQServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			DemoMQTT.this.mqs = ((MQBinder) service).getService();
			if (DemoMQTT.this.clientId != null && DemoMQTT.this.topic != null) {
				DemoMQTT.this.task.post(DemoMQTT.this.connectMQ);
			}
			DemoMQTT.this.mqsConnected = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			DemoMQTT.this.mqsConnected = false;
		}

	}

	private class MQConnectTask implements Runnable {

		@Override
		public void run() {
			SharedPreferences sp = DemoMQTT.this.getSharedPreferences(
					"PersistenceSharedPreferences", Context.MODE_PRIVATE);
			SharedPreferences.Editor spEditor = sp.edit();
			spEditor.putString("clientId", clientId).commit();
			spEditor.putString("topic", topic).commit();
			if (DemoMQTT.this.mqs.connect(clientId, topic)) {
				task.post(new Runnable() {
					@Override
					public void run() {
						NotificationManager mNotifMan = DemoMQTTResources
								.getNotificationManager();

						if (mNotifMan != null) {
							mNotifMan.cancel(0);
						}

						LogUtil.d(TAG,
								"User has logined... call js to exec handler...");

						try {
							SharedPreferences sp = DemoMQTT.this
									.getSharedPreferences(
											"PersistenceSharedPreferences",
											Context.MODE_PRIVATE);
							String message = sp.getString("message", "");
							if (StringUtils.isNotBlank(message)) {
								DemoMQTT.this
										.callMessageHandlerRouters(message);
							}
							SharedPreferences.Editor spEditor = sp.edit();
							spEditor.remove("message").commit();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			}
		}

	}

	/**
	 * 讓MQServeice在收到MQ資訊時，可以回call的Java Script函式．
	 * 
	 * @param appId
	 * @param type
	 * @param msg
	 */
	public void callMessageHandlerRouters(String msg) {
		StringBuilder st = new StringBuilder("showPushMsg('");
		st.append(msg).append("');");
		LogUtil.d(TAG, "callJs showPushMsg", st.toString());
		sendJavascript(st.toString());
	}

	/**
	 * 將MQTT連線狀態傳送到Javascript函式
	 * 
	 * @param connectStatus
	 */
	public void sendMqttConnectionStatus(String connectStatus) {
		StringBuilder st = new StringBuilder("showPushMsg('");
		st.append(connectStatus).append("');");
		LogUtil.d(TAG, "callJs showPushMsg ", st.toString());
		sendJavascript(st.toString());
	}

	/**
	 * 讓MQPlugin呼叫建立MQ連線的函式．
	 * 
	 * @param clientId
	 * @param topic
	 * @return
	 */
	public void subscribeTopic(String clientId, String topic) {
		this.clientId = clientId;// 將客戶登入的clientId及UID暫存在Activity裡,
									// 做為onResume重建連線用的.
		this.topic = topic;// 將客戶登入的clientId及UID暫存在Activity裡, 做為onResume重建連線用的.
		task.post(connectMQ);
	}

	/**
	 * 讓MQPlugin呼叫，傳送TOPIC資訊到MQ的函式．
	 * 
	 * @param topic
	 * @param message
	 * @param qos
	 */
	public void publishMessage(final String topic, final String message,
			final Integer qos) {
		task.post(new Runnable() {

			@Override
			public void run() {
				DemoMQTT.this.mqs.publishMessage(topic, message, qos);
			}
		});
	}
}
