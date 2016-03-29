/**
 * 
 */
package com.webcomm.wl.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import com.DemoMQTT.DemoMQTT;
import com.DemoMQTT.DemoMQTTResources;
import com.webcomm.util.LogUtil;

/**
 * @author DannyYang
 *
 */
public class MQTTPlugin extends CordovaPlugin {
	
	public static final String TAG=MQTTPlugin.class.getCanonicalName();
	private static final String CONNECT = "CONNECT";
	private static final String SEND_MSG = "SEND_MSG";
	private static final int DEFAULT_QOS = 2;

	@Override
	public boolean execute(String action, JSONArray arguments, CallbackContext callbackContext) throws JSONException {
		DemoMQTT demoMQTT=DemoMQTTResources.getDemoMQTT();
		if(CONNECT.equals(action)) {
			String clientId = arguments.getString(0);
			String topic = arguments.getString(1);
			demoMQTT.subscribeTopic(clientId, topic);
			callbackContext.success();
			return true;
		}else if(SEND_MSG.equals(action)){
			// 這邊的 TOPIC 不會經過特別處理
			String topic = arguments.getString(0);
			String message = arguments.getString(1);
			demoMQTT.publishMessage(topic, message, DEFAULT_QOS);
			callbackContext.success();
			return true;	
		}
		String error="UNKNOW action:"+action+" for MQPlugin.";
		callbackContext.error(error);
		LogUtil.w(TAG, error);
		return false;
	}

}
