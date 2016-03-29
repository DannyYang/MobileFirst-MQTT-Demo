package com.webcomm.mqtt;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.DemoMQTT.DemoMQTTResources;
import com.ibm.micro.client.mqttv3.MqttCallback;
import com.ibm.micro.client.mqttv3.MqttClient;
import com.ibm.micro.client.mqttv3.MqttConnectOptions;
import com.ibm.micro.client.mqttv3.MqttDeliveryToken;
import com.ibm.micro.client.mqttv3.MqttMessage;
import com.ibm.micro.client.mqttv3.MqttTopic;
import com.webcomm.util.LogUtil;

public class MQProxy {

	public static final String TAG = MQProxy.class.getName();
	private final ConnectivityManager aConnectionManager;

	private Timer timer;

	private static final String MQTT_URL = "tcp://YOUR_MQTT_IP_ADDRES:1883";
	private static final int MQTT_KEEP_ALIVE = 300;
	private static final boolean MQTT_CLEAN_SESSION = false;
	private static final int MQTT_RECONNECT_INTERVAL = 3 * 1000;
	private static final int MQTT_HEARTBEAT_INTERVAL = 30 * 1000;

	private MqttClient mqttClient = null;
	private boolean connected = false;

	private static MQProxy instance = null;

	private MQProxy() {
		this.aConnectionManager = DemoMQTTResources.getConnectivityManager();
	}

	public static MQProxy getInstance() {

		if (instance == null) {
			instance = new MQProxy();
		}
		return instance;
	}

	/**
	 * 檢查Service是否有建立MQTT的連線.
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}

	public void publishMessage(String topic, String message, Integer qos) {
		if (mqttClient != null) {
			MqttTopic t = mqttClient.getTopic(topic);

			// 建立訊息
			MqttMessage m = new MqttMessage(message.getBytes());
			m.setQos(qos);

			// 發佈訊息
			LogUtil.d(TAG, "Publishing to topic: \"", topic, " message:",
					message, "\" qos: ", qos.toString());
			try {
				MqttDeliveryToken token = t.publish(m);
				token.waitForCompletion(5000);
				System.out.println("Delivery token \"" + token.hashCode()
						+ "\" has been received: " + token.isComplete());
			} catch (Exception e) {
				LogUtil.e(TAG, e, "訊息推送失敗 : ", message);
			}
		} else {
			// TODO 必須加入錯誤的處理．
		}
	}

	/**
	 * 透過傳入的clientId建立mqtt連線, 再註冊topic
	 * 
	 * @param clientId
	 * @param topic
	 * @return
	 */
	public synchronized boolean connect(String clientId, String topic,
			MsgArrivedCallback callback) {
		try {
			if (!DemoMQTTResources.isService()) {
				SharedPreferences sp = DemoMQTTResources.getContext()
						.getSharedPreferences("PersistenceSharedPreferences",
								Context.MODE_PRIVATE);
				String status = sp.getString("ISFOREGROUND", "");

				// 因為第一次安裝的時候onResume當時無法寫檔,所以抓到的會是 null
				if (status == null) {
				} else {
					if ("TRUE".compareTo(status) != 0)
						return false;
				}
			} else {
				SharedPreferences sp = DemoMQTTResources.getContext()
						.getSharedPreferences("PersistenceSharedPreferences",
								Context.MODE_PRIVATE);
				String status = sp.getString("ISFOREGROUND", "");

				if ("FALSE".compareTo(status) != 0)
					return false;
			}

			if (isNetworkAvailable()) {
				disconnect();
				// 第三個參數要傳入null, 或是開啟寫檔案的權限
				mqttClient = new MqttClient(MQTT_URL, clientId, null);
				mqttClient
						.setCallback(new MQCallback(clientId, topic, callback));
				MqttConnectOptions conOptions = new MqttConnectOptions();
				conOptions.setCleanSession(MQTT_CLEAN_SESSION);
				conOptions.setKeepAliveInterval(MQTT_KEEP_ALIVE);
				mqttClient.connect(conOptions);
				mqttClient.subscribe(topic, 1);

				connected = true;

				LogUtil.d(TAG, "MQtt Connected to server:", MQTT_URL,
						" with client id:", clientId);
				LogUtil.d(TAG, "Subscribe topic:", topic, " to MQTT Server.");
			} else {
				LogUtil.d(TAG, "Network unavailable, can't connect MQServer.");
				disconnect();
				scheduleReconnect(clientId, topic, callback);
			}
		} catch (Exception e) {
			if (mqttClient == null) {
				LogUtil.d(TAG, "mqttclient is nulll. ", clientId);

			}
			LogUtil.e(TAG, e, "Error while connect to MQServer:", MQTT_URL,
					" with clientID :", clientId);
			disconnect();
			scheduleReconnect(clientId, topic, callback);

		}

		if (!DemoMQTTResources.isService())
			DemoMQTTResources.getDemoMQTT().sendMqttConnectionStatus(
					String.valueOf(connected));

		return connected;
	}

	/**
	 * 中斷mqtt的連線.
	 */
	public void disconnect() {
		if (mqttClient != null && mqttClient.isConnected()) {
			try {
				LogUtil.d(TAG, "Disconnected to MQtt server:", MQTT_URL);
				mqttClient.disconnect();
			} catch (Exception e) {
				LogUtil.e(TAG, e, "Error while disconnect to MQServer:",
						MQTT_URL);
			}
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		mqttClient = null;
		connected = false;
		// disconnectFlag = false;
		/*
		 * if (!DemoMQTTResources.isService())
		 * DemoMQTTResources.getDemoMQTT().sendMqttConnectionStatus(
		 * String.valueOf(connected));
		 */
	}

	/**
	 * 檢查網路是否連通
	 * 
	 * @return
	 */
	private boolean isNetworkAvailable() {
		NetworkInfo info = aConnectionManager.getActiveNetworkInfo();
		if (info == null) {
			return false;
		}
		return info.isConnected();
	}

	public void scheduleReconnect(String clientId, String topic,
			MsgArrivedCallback callback) {
		LogUtil.d(TAG, "scheduleReconnect to MQtt server:", MQTT_URL);
		if (timer == null) {
			timer = new Timer(true);
		}
		timer.schedule(new ReconnectTask(clientId, topic, callback),
				MQTT_RECONNECT_INTERVAL);
	}

	public String getMQTT_URL() {
		return MQTT_URL;
	}

	public int getMQTT_KEEP_ALIVE() {
		return MQTT_KEEP_ALIVE;
	}

	public boolean isMQTT_CLEAN_SESSION() {
		return MQTT_CLEAN_SESSION;
	}

	public int getMQTT_RECONNECT_INTERVAL() {
		return MQTT_RECONNECT_INTERVAL;
	}

	public int getMQTT_HEARTBEAT_INTERVAL() {
		return MQTT_HEARTBEAT_INTERVAL;
	}

	private class MQCallback implements MqttCallback {

		private final MQProxy that = MQProxy.this;
		private final String clientId;
		private final String topic;
		private final MsgArrivedCallback callback;

		public MQCallback(String clientId, String topic,
				MsgArrivedCallback callback) {
			this.clientId = clientId;
			this.topic = topic;
			this.callback = callback;

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.ibm.micro.client.mqttv3.MqttCallback#connectionLost(java.lang
		 * .Throwable) 斷線時註冊重新連線的排程.
		 */
		@Override
		public void connectionLost(Throwable arg0) {
			LogUtil.w(TAG, "MQTT lost from server:", that.getMQTT_URL());
			connected = false;

			if (!DemoMQTTResources.isService())
				DemoMQTTResources.getDemoMQTT().sendMqttConnectionStatus(
						String.valueOf(connected));

			if (mqttClient != null && !mqttClient.isConnected()) {
				that.scheduleReconnect(clientId, topic, callback);
			}
		}

		@Override
		public void deliveryComplete(MqttDeliveryToken arg0) {

		}

		@Override
		public void messageArrived(MqttTopic topic, MqttMessage message)
				throws Exception {
			LogUtil.d(TAG, "Topic:\t\t", topic.getName());
			String s = new String(message.getPayload());
			LogUtil.d(TAG, "Message:\t", s);
			LogUtil.d(TAG, "QoS:\t\t" + message.getQos());
			callback.messageArrived(s);
		}

	}

	private class ReconnectTask extends TimerTask {

		private final String clientId;
		private final String topic;
		private final MsgArrivedCallback callback;

		public ReconnectTask(String clientId, String topic,
				MsgArrivedCallback callback) {
			this.clientId = clientId;
			this.topic = topic;
			this.callback = callback;
		}

		@Override
		public void run() {
			LogUtil.d(TAG, "ReconnectTask: connect to MQtt server:", MQTT_URL);
			MQProxy.this.connect(clientId, topic, callback);
		}

	}

	// push Heartbeat to MQTT Server
	private class HeartbeatTask extends TimerTask {
		public HeartbeatTask() {
		}

		@Override
		public void run() {
			MQProxy.this.publishMessage("mportal/SERVER/HEARTBEAT",
					"Heartbeat", 0);
		}
	}
}
