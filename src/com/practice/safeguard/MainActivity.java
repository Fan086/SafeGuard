package com.practice.safeguard;

import com.practice.safeguard.utils.Constants;
import com.practice.safeguard.utils.SpUtils;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements OnItemClickListener, OnClickListener {
	private GridView gv_menus;//主界面的按钮
	
	private int icons[] = {R.drawable.safe,R.drawable.callmsgsafe,R.drawable.item_gv_selector_app
			,R.drawable.taskmanager,R.drawable.netmanager,R.drawable.trojan
			,R.drawable.sysoptimize,R.drawable.atools,R.drawable.settings};
	
	private String names[]={"手机防盗","通讯卫士","软件管家","进程管理","流量统计","病毒查杀","缓存清理","高级工具","设置中心"};

	private MyAdapter adapter;//gridview的适配器

	private AlertDialog dialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//初始化界面
		initView();
		
		//初始化适配器
		initAdapter();
		
		//初始化对话框
		initDialog();
	}
	private void initDialog() {
		
		View view = View.inflate(this, R.layout.dialog_main_setting_pwd, null);
		dialog = new AlertDialog.Builder(this)
				//为dialog使用自定义view
				.setView(view)
				.create();
		
		final EditText et_setting_pwd_one = (EditText) view.findViewById(R.id.et_dialog_setting_password_passone);
		final EditText et_setting_pwd_two = (EditText) view.findViewById(R.id.et_dialog_setting_password_passtwo);
		
		//局部内部类，只用于给dialog的按钮做处理
		class DialogOnClickListener implements OnClickListener{
			@Override
			public void onClick(View v) {
				switch(v.getId()){
					case R.id.bt_dialog_setting_password_setpass:
						String pwd1 = et_setting_pwd_one.getText().toString();
						String pwd2 = et_setting_pwd_two.getText().toString();
						if(TextUtils.isEmpty(pwd1) || TextUtils.isEmpty(pwd2)) {
							Toast.makeText(MainActivity.this, "密码不能为空", 0).show();
							break;
						}
						if(!TextUtils.equals(pwd1, pwd2)){
							Toast.makeText(MainActivity.this, "请确保密码一致", 0).show();
							break;
						}
						//将密码存到sp中
						SpUtils.putString(MainActivity.this, Constants.PWD, pwd1);
						Toast.makeText(MainActivity.this, "保存密码成功", 0).show();
						dialog.dismiss();
						break;
					case R.id.bt_dialog_setting_password_cancel:
						dialog.dismiss();
						break;
				}
			}
		}
		OnClickListener dialogListener = new DialogOnClickListener();
		
		view.findViewById(R.id.bt_dialog_setting_password_setpass).setOnClickListener(dialogListener);
		view.findViewById(R.id.bt_dialog_setting_password_cancel).setOnClickListener(dialogListener);
		
	}
	private void initAdapter() {
		adapter = new MyAdapter();
		
		gv_menus.setAdapter(adapter);
	}
	private void initView() {
		setContentView(R.layout.activity_main);
		
		gv_menus = (GridView) findViewById(R.id.gv_main_menus);
		
		gv_menus.setOnItemClickListener(this);
	}
	private class MyAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return icons.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null){
				convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_main_gv_menu, parent, false);
			}
			ImageView iv_item_gv_icon = (ImageView) convertView.findViewById(R.id.iv_item_main_gv_icon);
			TextView tv_item_gv_name = (TextView) convertView.findViewById(R.id.tv_item_main_gv_name);
			
			//为每个item的控件赋值
			iv_item_gv_icon.setImageResource(icons[position]);
			tv_item_gv_name.setText(names[position]);
			
			return convertView;
		}
		
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch(position){
			
			case 0:
				dialog.show();
				break;
		}
	}
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
}
