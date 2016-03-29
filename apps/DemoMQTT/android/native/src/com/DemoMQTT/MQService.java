/**
 * 
 */
package com.DemoMQTT;

import com.webcomm.mqtt.MQProxy;
import com.webcomm.mqtt.MsgArrivedCallback;
import com.webcomm.util.LogUtil;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * MQService是作為MPortal主程式在前景執行時，背後處理MQTT連線的服務(在同一Process執行的程式).
 * 如果MPortal在背景執行時，將切換到{@link StandAlongMQService} 執行．
 * 
 * @author DannyYang
 *
 */
public class MQService extends Service {

	public static final String TAG = MQService.class.getName();
	private final IBinder mBinder = new MQBinder();
	private final MsgCallback callback = new MsgCallback();
	private MQProxy proxy;

	@Override
	public IBinder onBind(Intent intent) {
		LogUtil.d(TAG, "MQService onBind.");
		proxy = MQProxy.getInstance();
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		LogUtil.d(TAG, "MQService onUnbind.");
		disconnect();
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		LogUtil.d(TAG, "MQService onDestroy.");
		disconnect();
		super.onDestroy();
	}

	public void publishMessage(String topic, String message, Integer qos) {
		if (proxy != null) {
			proxy.publishMessage(topic, message, qos);
		}
	}

	public void disconnect() {
		if (proxy != null) {
			LogUtil.d(TAG, "Disconect MQProxy.");
			proxy.disconnect();
		}
	}

	/**
	 * 透過傳入的clientId建立mqtt連線, 並註冊topic
	 * 
	 * @param clientId
	 * @param topic
	 * @return
	 */
	public boolean connect(String clientId, String topic) {
		// KenTsai start 註冊清檔的動作必須要做在Connect之前,因為要判斷內容是否為前日資訊要清除
		boolean result = proxy.connect(clientId, topic, callback);

		return result;
	}

	public class MQBinder extends Binder {

		public MQService getService() {
			return MQService.this;
		}
	}

	public class MsgCallback extends MsgArrivedCallback {

		private final DemoMQTT demoMQTT = DemoMQTTResources.getDemoMQTT();

		@Override
		public void messageArrived(String msg) {
			if (msg != null) {
				demoMQTT.callMessageHandlerRouters(msg);
			} else {
				LogUtil.e(TAG, "Message ERROR! ");
			}
		}

	}

}
