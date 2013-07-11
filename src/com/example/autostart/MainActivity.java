package com.example.autostart;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private ListView lv;          
	private Button allowBt;       //允许列表显示按钮
	private Button forbidBt;      //禁止列表显示按钮
	private Button goBt;			//优化执行按钮
	private Context context;
	private PackageManager mPackageManager;
	private MyAdapter mAdapter;
	private ProgressDialog progressDialog;
	Intent intent;
	private ArrayList<HashMap<String, Object>> allowList;			//允许自启动应用信息保存
	private ArrayList<HashMap<String, Object>> forbidList;			//禁止自启动应用信息保存
	private List<ResolveInfo> allowInfoList; 						//获取自启动receiver的信息
	private List<ResolveInfo> forbidInfoList; 						//获取包含被禁止自启动receiver的信息
	private int flag;												//allowBt和forbidBt的点击记录，点击allowBt时为0，点击forbidBt时为1
	protected static final int START = 0;							//显示progressdiaglog
	protected static final int STOP = 1;							//关闭progressdiaglog
	protected static final int PROCESS = 2;							//progressdiaglog
	private static HashMap<Integer,Boolean> isSelected; 			//保存checkbox的点击信息
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//各种初始化
		flag = 0;
		context = this;
		lv = (ListView) findViewById(R.id.lv);
		allowBt = (Button) findViewById(R.id.allow_bt);
		forbidBt = (Button) findViewById(R.id.forbid_bt);
		goBt = (Button) findViewById(R.id.go_bt);
		progressDialog = new ProgressDialog(MainActivity.this);  
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);	//设置progressdiaglog风格为圆形进度条  
		progressDialog.setTitle("提示");									//设置progressdiaglog标题
		progressDialog.setMessage("优化中...");  
		progressDialog.setCancelable(false);							//设置progressdiaglog进度条是否可以按退回键取消  
		mPackageManager = getPackageManager();
		intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
		allowList = new ArrayList<HashMap<String, Object>>();
		forbidList = new ArrayList<HashMap<String, Object>>();
		updateAllowList();												//更新允许自启动应用列表
		mAdapter = new MyAdapter(allowList,context);
		lv.setAdapter(mAdapter);										//为lv设置适配器
		lv.setItemsCanFocus(false);
		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);				//设置lv可以为多选
		//设置lv中每一个item的点击动作，点击每一个item后会点亮对应的checkbox
		lv.setOnItemClickListener(new OnItemClickListener() {			
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position,long id) {  
	        	// 取得ViewHolder对象，这样就省去了通过层层的findViewById去实例化我们需要的cb实例的步骤  
	        	ViewHolder holder = (ViewHolder) view.getTag();  
	            // 改变CheckBox的状态  
	            holder.cb.toggle();  
	            // 将CheckBox的选中状况记录下来  
	            MyAdapter.getIsSelected().put(position, holder.cb.isChecked());   
	        }  
	    });  
		//设置allowBt的点击动作，更新allowList，并且更新lv的显示
		allowBt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				flag = 0;
				updateAllowList();
				mAdapter.refresh(allowList);
			}
		});
		//设置forbidBt的点击动作，更新forbidList，并且更新lv的显示
		forbidBt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				flag = 1;
				updateForbidList();
				mAdapter.refresh(forbidList);
			}
		});
		//设置goBt的点击动作，对选中的应用进行优化操作，并且更新lv的显示
		goBt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				//创建新进程，执行优化操作
				new Thread(){
				public void run(){	
					Message msg = new Message();
					msg.what = START;
					//对mHandler发送msg，提示开始显示进度条
					mHandler.sendMessage(msg);			
					String cmd;
					//获取checkbox的选择信息
					//allowBt被点击，即当前lv显示是允许自启动应用列表时，进行禁止选中应用自启动的操作
					if(flag == 0){				
						isSelected = mAdapter.getIsSelected();
						for(int i = 0; i < allowList.size(); i++){
							//寻找checkbox被选中的应用
							if(isSelected.get(i)){		
								//获取该应用包含的packagereceiver，格式为”package/receiver“
								String packageReceiverList[] = allowList.get(i).get("packageReceiver").toString().split(";");	
								//发送当前优化的应用名称
								Message msg1 = new Message();
								msg1.obj = allowList.get(i).get("appName");
								msg1.what = PROCESS;
								mHandler.sendMessage(msg1);
								//disable这些receiver
								for(int j = 0; j < packageReceiverList.length; j++){
									cmd = "pm disable "+packageReceiverList[j];
									//部分receiver包含$符号，需要做进一步处理，用"$"替换掉$
									cmd = cmd.replace("$", "\""+"$"+"\"");
									//执行命令
									execCmd(cmd);			
								}
							}
						}
						updateAllowList();		//更新allowList
					//forbidBt被点击，即当前lv显示被禁止自启动应用列表时，进行恢复选中应用自启动的操作
					}else{
						isSelected = mAdapter.getIsSelected();
						for(int i = 0; i < forbidList.size(); i++){
							if(isSelected.get(i)){
								String packageReceiverList[] = forbidList.get(i).get("packageReceiver").toString().split(";");
								//发送当前优化的应用名称
								Message msg1 = new Message();
								msg1.obj = forbidList.get(i).get("appName");
								msg1.what = PROCESS;
								mHandler.sendMessage(msg1);
								for(int j = 0; j < packageReceiverList.length; j++){
									cmd = "pm enable "+packageReceiverList[j];
									cmd = cmd.replace("$", "\""+"$"+"\"");
									execCmd(cmd);			
								}
							}
						}
						updateForbidList();
					}
					Message msg2 = new Message();
					msg2.what = STOP;
					//对mHandler发送msg，提示关闭进度条
					mHandler.sendMessage(msg2);
				}
				}.start();
			}
		});
	}
	
	//用于配合thread的UI更新，包括显示进度条，关闭进度条，刷新listview显示
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
			case START: progressDialog.show(); 
						break;
			case STOP: progressDialog.cancel(); 
					   if(flag == 0)
						   mAdapter.refresh(allowList);
					   else
						   mAdapter.refresh(forbidList); 
					   break;
			case PROCESS: progressDialog.setMessage(msg.obj.toString()+"优化中...");
							break;

			}
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	//allowList的更新操作
	public void updateAllowList(){
		//获取自启动receiver的信息
		allowInfoList = mPackageManager.queryBroadcastReceivers(intent,PackageManager.GET_RECEIVERS);
		int k = 0;
		//去除系统应用receiver
		while(k < allowInfoList.size()){
			if((allowInfoList.get(k).activityInfo.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==1||  
					(allowInfoList.get(k).activityInfo.applicationInfo.flags&ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)==1){ 
				allowInfoList.remove(k);
			}else
				k++;
		}
		//清空allowList
		allowList.clear();
		String appName = null;
		String packageReceiver = null;
		Object icon = null;
		//获取allowInfoList中第一个receiver对应的应用的名称
		if(allowInfoList.size() > 0){
			appName = mPackageManager.getApplicationLabel(allowInfoList.get(0).activityInfo.applicationInfo).toString();
			//获取allowInfoList中第一个receiver对应的应用的包名和receiver名称，格式为"package/receiver"
			packageReceiver = allowInfoList.get(0).activityInfo.packageName + "/" + allowInfoList.get(0).activityInfo.name;
			//获取allowInfoList中第一个receiver对应的应用的图标信息
			icon =  mPackageManager.getApplicationIcon(allowInfoList.get(0).activityInfo.applicationInfo);
			for(int i = 1; i < allowInfoList.size(); i++){ 
				//保存应用信息
				HashMap<String, Object> map = new HashMap<String, Object>();
				//由于一个应用可能包含多个receiver，需要将这些receiver和对应的应用名称放入同一个map中，对于这些不同的receiver用";"隔开，以便之后用split方法取出
				if(appName.equals(mPackageManager.getApplicationLabel(allowInfoList.get(i).activityInfo.applicationInfo).toString())){
					packageReceiver = packageReceiver + ";" + allowInfoList.get(i).activityInfo.packageName + "/" + allowInfoList.get(i).activityInfo.name;
					//如果当前的receiver和之前的receiver对应的是不同的应用，那么将之前的应用信息保存到map中，然后存储到allowList中。
				}else{
					map.put("icon", icon);
					map.put("appName", appName);
					map.put("packageReceiver", packageReceiver);
					allowList.add(map);
					packageReceiver = allowInfoList.get(i).activityInfo.packageName + "/" + allowInfoList.get(i).activityInfo.name;
					appName = mPackageManager.getApplicationLabel(allowInfoList.get(i).activityInfo.applicationInfo).toString();
					icon =  mPackageManager.getApplicationIcon(allowInfoList.get(i).activityInfo.applicationInfo);
				}
			}
			//将allowInfoList中的最后一个应用信息保存到allowList中
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("icon", icon);
			map.put("appName", appName);
			map.put("packageReceiver", packageReceiver);
			allowList.add(map);
		}	
	}
	
	//forbidList的更新操作
	public void updateForbidList(){
		//获取包含被禁止自启动receiver的信息
		forbidInfoList = mPackageManager.queryBroadcastReceivers(intent,PackageManager.GET_DISABLED_COMPONENTS);
		int k = 0;
		//去除系统应用receiver以及允许自启动的receiver
		while(k < forbidInfoList.size()){
			if((forbidInfoList.get(k).activityInfo.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==1||  
					(forbidInfoList.get(k).activityInfo.applicationInfo.flags&ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)==1){ 
				forbidInfoList.remove(k);
			}else
				k++;
		}
		k = 0;
		while(k < forbidInfoList.size()){
			 ComponentName mComponentName = new ComponentName(forbidInfoList.get(k).activityInfo.packageName, forbidInfoList.get(k).activityInfo.name);
			 if(mPackageManager.getComponentEnabledSetting(mComponentName)!=2)
				 forbidInfoList.remove(k);
			 else
				 k++;
		}
		forbidList.clear();
		String appName = null;
		String packageReceiver = null;
		Object icon = null;
		if(forbidInfoList.size() > 0){
			appName = mPackageManager.getApplicationLabel(forbidInfoList.get(0).activityInfo.applicationInfo).toString();
			//获取forbidInfoList中第一个receiver对应的应用的包名和receiver名称，格式为"package/receiver"
			packageReceiver = forbidInfoList.get(0).activityInfo.packageName + "/" + forbidInfoList.get(0).activityInfo.name;
			//获取forbidInfoList中第一个receiver对应的应用的图标信息
			icon =  mPackageManager.getApplicationIcon(forbidInfoList.get(0).activityInfo.applicationInfo);
			for(int i = 1; i < forbidInfoList.size(); i++){
				HashMap<String, Object> map = new HashMap<String, Object>(); 
				//由于一个应用可能包含多个receiver，需要将这些receiver和对应的应用名称放入同一个map中，对于这些不同的receiver用";"隔开，以便之后用split方法取出
				if(appName.equals(mPackageManager.getApplicationLabel(forbidInfoList.get(i).activityInfo.applicationInfo).toString())){
					packageReceiver = packageReceiver + ";" + forbidInfoList.get(i).activityInfo.packageName + "/" + forbidInfoList.get(i).activityInfo.name;
					//如果当前的receiver和之前的receiver对应的是不同的应用，那么将之前的应用信息保存到map中，然后存储到forbidList中。				
					}else{
						map.put("icon", icon);
						map.put("appName", appName);
						map.put("packageReceiver", packageReceiver);
						forbidList.add(map);
						packageReceiver = forbidInfoList.get(i).activityInfo.packageName + "/" + forbidInfoList.get(i).activityInfo.name;
						appName = mPackageManager.getApplicationLabel(forbidInfoList.get(i).activityInfo.applicationInfo).toString();
						icon =  mPackageManager.getApplicationIcon(forbidInfoList.get(i).activityInfo.applicationInfo);
					}
				}
			//将forbidInfoList中的最后一个应用信息保存到forbidList中,position+1与forbidInfoList大小相等时，表示
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("icon", icon);
			map.put("appName", appName);
			map.put("packageReceiver", packageReceiver);
			forbidList.add(map);
		}
	}
	
	//用root权限执行外部命令"pm disable"和"pm enable"
	public static boolean execCmd(String cmd) {
	    Process process = null;
	    DataOutputStream os = null;
	    try {
	        process = Runtime.getRuntime().exec("su"); //切换到root帐号
	        os = new DataOutputStream(process.getOutputStream());
	        os.writeBytes(cmd + "\n");
	        os.writeBytes("exit\n");
	        os.flush();
	        process.waitFor();
	    } catch (Exception e) {
	        return false;
	    } finally {
	        try {
	            if (os != null) {
	                os.close();
	            }
	            process.destroy();
	        } catch (Exception e) {
	        }
	    }
	    return true;
	}

}
