package com.webcomm.util;

import android.util.Log;

/**
 * 包裝android Log的元件, 讓我們有機會將log的輸出關閉掉, 以提高效能.
 * 
 * @author DannyYang
 *
 */
public class LogUtil {

	private static boolean show = true;
	
	/**
	 * Logs the data to the android Log.v . 
	 * 
	 * @param tag
	 * @param msg
	 */
	public static void v(String tag, String... msg){
		if(show){
			Log.v(tag, toString(msg));
		}
	}
	
	/**
	 * Logs the data to the android Log.d . 
	 * 
	 * @param tag
	 * @param msg
	 */
	public static void d(String tag, String... msg) {
		if(show) {
			Log.d(tag, toString(msg));
		}
	}
	
	/**
	 * Logs the data to the android Log.i . 
	 * 
	 * @param tag
	 * @param msg
	 */
	public static void i(String tag, String... msg) {
		if(show) {
			Log.i(tag, toString(msg));
		}
	}
	
	/**
	 * Logs the data to the android Log.e . 
	 * 
	 * @param tag
	 * @param msg
	 */
	public static void e(String tag, String... msg) {
		if(show) { 
			Log.e(tag, toString(msg));
		}
	}
	
	/**
	 * Logs the data to the android Log.e . 
	 * 
	 * @param tag
	 * @param msg
	 * @param e
	 */
	public static void e(String tag, Throwable e, String... msg) {
		if(show) { 
			Log.e(tag, toString(msg), e);
		}
	}
	
	/**
	 * Logs the data to the android Log.w . 
	 * 
	 * @param tag
	 * @param msg
	 */
	public static void w(String tag, String... msg) {
		if(show) {
			Log.w(tag, toString(msg));
		}
	}
	
	private static String toString(String... msg){
		if(msg==null||msg.length==0){
			return "";
		}
		StringBuilder sb=new StringBuilder();
		for(String m:msg){
			if(msg!=null){
				sb.append(m);
			}else{
				sb.append("null");
			}
		}
		return sb.toString();
	}
}
