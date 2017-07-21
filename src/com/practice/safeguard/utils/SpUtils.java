package com.practice.safeguard.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtils {
	public static void putString(Context context, String key, String value){
		SharedPreferences sp = context.getSharedPreferences(Constants.CONFIG, Context.MODE_PRIVATE);
		sp.edit().putString(key, value).apply();
	}
	
	public static String getString(Context context, String key, String defaultValue){
		SharedPreferences sp = context.getSharedPreferences(Constants.CONFIG, Context.MODE_PRIVATE);
		return sp.getString(key, defaultValue);
	}
}
