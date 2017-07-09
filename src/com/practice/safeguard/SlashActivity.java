package com.practice.safeguard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.practice.safeguard.domain.VersionBean;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SlashActivity extends Activity implements AnimationListener {
	
	private static final String VERSION_URL = "http://10.0.2.2:8080/SaveguardServer/version_info.json";
	protected static final int GOTO_MAIN = 0x00;
	protected static final int DOWNLOAD_NEW_VERSION = 0x01;
	
	private static class MyHandler extends Handler{
		private WeakReference<SlashActivity> mActivity;
		
		public MyHandler(SlashActivity activity){
			mActivity = new WeakReference<>(activity);
		}
		public void handleMessage(Message msg) {
			SlashActivity activity = mActivity.get();
			if(activity == null){
				return;
			}
			switch(msg.what){
				case GOTO_MAIN:
					activity.startActivity(new Intent(activity, MainActivity.class));
					activity.finish();
					break;
				case DOWNLOAD_NEW_VERSION:
					VersionBean versionBean = (VersionBean) msg.obj;
					activity.showUpdateDialog(versionBean);
					break;
			}
		}

	};
	
	private Handler mHandler = new MyHandler(this);
	
	private TextView tv_versionName;
	private RelativeLayout rl_slash_container;
	private volatile int currentVersion;
	private volatile VersionBean versionBean;
	
	private void showUpdateDialog(VersionBean versionBean) {
		AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle("发现新版本")
				.setMessage(versionBean.getDesc())
				.setNegativeButton("更新", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						System.out.println("正在为您更新。。");
						dialog.dismiss();
					}
				}).setPositiveButton("稍后", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(SlashActivity.this, MainActivity.class));
						dialog.dismiss();
						finish();
					}
				}).setCancelable(false)
				.create();
		
		dialog.show();
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//初始化布局
		initView();
		
		//初始化动画
		initAnimation();
		
		//检测版本信息
		checkVersionInfo();
	}

	private void checkVersionInfo() {
		//获取当前版本
		currentVersion = getCurrentVersion();
		//开启一个线程访问网络
		new Thread(){
			public void run() {
				try {
					//建立连接
					URL url = new URL(VERSION_URL);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setConnectTimeout(3000);
					conn.setReadTimeout(3000);
					conn.setRequestMethod("GET");
					int responseCode = conn.getResponseCode();
					//连接成功
					if(responseCode == 200){
						//从流对象中获取json字符串
						InputStream is = conn.getInputStream();
						BufferedReader br = new BufferedReader(new InputStreamReader(is));
						String line = null;
						StringBuilder sb = new StringBuilder();
						while((line = br.readLine()) != null){
							sb.append(line);
						}
						//将json字符串解析成bean对象
						versionBean = parseJson(sb.toString());
						
						//在onAnimationEnd中调用比较版本的方法
						
					}else{
						Log.d(getClass().getSimpleName(), "连接建立失败");
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			

			private VersionBean parseJson(String string) {
				VersionBean versionBean = new VersionBean();
				try {
					JSONObject root = new JSONObject(string);
					versionBean.setVersion(root.getInt("version"));
					versionBean.setUrl(root.getString("url"));
					versionBean.setDesc(root.getString("desc"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				return versionBean;
			};
		}.start();
	}
	
	private void compareTwoVersion(int currentVersion, VersionBean versionBean) {
		//根据版本是否相同，发送相应的消息
		Message msg = Message.obtain();
		int newVersion = versionBean.getVersion();
		
		Log.d(getClass().getSimpleName(), "当前版本" + currentVersion + "  新版本：" + newVersion);
		//如果相同，则进入主界面；否则，到网上下载最新版
		if(currentVersion == newVersion){
			msg.what = GOTO_MAIN;
			
		}else{
			msg.what = DOWNLOAD_NEW_VERSION;
			msg.obj = versionBean;
		}
		//发送消息给handler
		mHandler.sendMessage(msg);
	}
	private int getCurrentVersion() {
		int version = -1;
		PackageManager pm = getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
			version = pi.versionCode;
			
			//显示当前版本名
			tv_versionName.setText(pi.versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return version;
	}

	private void initAnimation() {
		//动画集
		AnimationSet as = new AnimationSet(false);
		//为动画集添加回调，等它执行完成后才可进行下一步的操作
		as.setAnimationListener(this);
		
		//渐变动画
		AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f); 
		aa.setDuration(3000);
		aa.setFillAfter(true);
		
		//缩放动画
		ScaleAnimation sa = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 
				0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		sa.setDuration(3000);
		sa.setFillAfter(true);
		
		//旋转动画
		RotateAnimation ra = new RotateAnimation(0,	360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		ra.setDuration(3000);
		ra.setFillAfter(true);
		
		//添加到动画集
		as.addAnimation(aa);
		as.addAnimation(sa);
		as.addAnimation(ra);
		
		//应用动画
		rl_slash_container.startAnimation(as);
		
	}

	private void initView() {
		setContentView(R.layout.activity_slash);
		
		tv_versionName = (TextView) findViewById(R.id.tv_slash_version_name);
		rl_slash_container = (RelativeLayout) findViewById(R.id.rl_slash_container);
	}
	@Override
	public void onAnimationStart(Animation animation) {
		
	}
	@Override
	public void onAnimationEnd(Animation animation) {
		compareTwoVersion(currentVersion, versionBean);
	}
	@Override
	public void onAnimationRepeat(Animation animation) {
	}
	
}
