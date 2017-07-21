package com.practice.safeguard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.practice.safeguard.domain.VersionBean;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SlashActivity extends Activity implements AnimationListener {
	
	private static final String VERSION_URL = "http://10.0.2.2:8080/SaveguardServer/version_info.json";
	protected static final int GOTO_MAIN = 0x00;
	protected static final int DOWNLOAD_NEW_VERSION = 0x01;
	protected static final int ERROR = 0x03;
	
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
					activity.gotoMain(activity);
					break;
				case DOWNLOAD_NEW_VERSION:
					VersionBean versionBean = (VersionBean) msg.obj;
					activity.showUpdateDialog(versionBean);
					break;
				//对各种错误进行集中处理
				case ERROR:
					switch(msg.arg1){
						case 404:
							Toast.makeText(activity, "连接异常", 0).show();
							break;
						case 4002:
							Toast.makeText(activity, "4002json解析错误", 0).show();
							break;
						case 4003:
							Toast.makeText(activity, "4003url", 0).show();
							break;
						case 4004:
							Toast.makeText(activity, "4004io", 0).show();
							break;
						case 4005:
							Toast.makeText(activity, "4005null", 0).show();
							break;
						default:
							Toast.makeText(activity, "其他代码错误" + msg.arg1, 0).show();
							break;
					}
					activity.gotoMain(activity);
					break;
			}
		}

	};
	private void gotoMain(SlashActivity activity) {
		activity.startActivity(new Intent(activity, MainActivity.class));
		activity.finish();
	}
	
	private Handler mHandler = new MyHandler(this);
	
	private TextView tv_versionName;
	private RelativeLayout rl_slash_container;
	private volatile int currentVersion;
	private volatile VersionBean versionBean;
	private ProgressBar pb_slash_download;
	
	private void showUpdateDialog(final VersionBean versionBean) {
		AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle("发现新版本")
				.setMessage(versionBean.getDesc())
				.setNegativeButton("更新", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						System.out.println("正在为您更新。。");
						
						
						String path = new File(Environment.getExternalStorageDirectory(),"SafeGuard.apk").getAbsolutePath();
						//下载文件到本地,并发送意图启动packageInstaller来安装
						downloadFile(versionBean.getUrl(), path);
						
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
	
	
	
	/**
	 * 在发现新版本后，下载新的apk到本地，并进行安装
	 * @param url
	 * @param path
	 * @return
	 */
	protected void downloadFile(String url, String path) {
		new HttpUtils().download(url, path, new RequestCallBack<File>() {
			
			@Override
			public void onLoading(long total, long current, boolean isUploading) {
				pb_slash_download.setVisibility(View.VISIBLE);
				pb_slash_download.setMax((int) total);
				pb_slash_download.setProgress((int) current);
				super.onLoading(total, current, isUploading);
			}

			@Override
			public void onFailure(HttpException arg0, String arg1) {
				Log.d(getClass().getSimpleName(), "下载文件失败" + arg0.toString());
				pb_slash_download.setVisibility(View.GONE);
			}

			@Override
			public void onSuccess(ResponseInfo<File> arg0) {
				Log.d(getClass().getSimpleName(), "下载文件成功");
				pb_slash_download.setVisibility(View.GONE);
				
				//将文件路径传入，进行安装
				installApk(arg0.result);
			}

			private void installApk(File file) {
				//发送意图启动packageInstaller来安装
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivityForResult(intent, 0);
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 0){
			gotoMain(this);
		}
	}
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
						Log.d(getClass().getSimpleName(), "发生了错误：" + responseCode);
						sendErrorMsg(ERROR, responseCode);
					}
				} catch (MalformedURLException e) {
					sendErrorMsg(ERROR, 4003);//代表url异常
				} catch (IOException e) {
					sendErrorMsg(ERROR, 4004);//代表io连接异常
				} catch (JSONException e) {
					sendErrorMsg(ERROR, 4002);//代表json解析错误
				} catch(NullPointerException e){
					sendErrorMsg(ERROR, 4005);
				}
			}



			private void sendErrorMsg(int what, int errorCode) {
				Message msg = Message.obtain();
				msg.what = what;
				msg.arg1 = errorCode;
				mHandler.sendMessage(msg);
			}

			

			private VersionBean parseJson(String string) throws JSONException{
				VersionBean versionBean = new VersionBean();
				JSONObject root = new JSONObject(string);
				versionBean.setVersion(root.getInt("version"));
				versionBean.setUrl(root.getString("url"));
				versionBean.setDesc(root.getString("desc"));
				
				return versionBean;
			};
		}.start();
	}
	
	private void compareTwoVersion(int currentVersion, VersionBean versionBean) {
		if(versionBean == null){
			Log.d(getClass().getSimpleName(), "versionBean为空");
			return;
		}
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
		pb_slash_download = (ProgressBar) findViewById(R.id.pb_slash_download);
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
