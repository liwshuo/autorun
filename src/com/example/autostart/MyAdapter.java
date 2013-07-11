package com.example.autostart;

import java.util.ArrayList;  
import java.util.HashMap;  
  
import android.content.Context;  
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;  
import android.view.View;  
import android.view.ViewGroup;  
import android.widget.BaseAdapter;  
import android.widget.CheckBox;  
import android.widget.TextView;  
import android.widget.ImageView;
  
public class MyAdapter extends BaseAdapter{  
    // 填充数据的list  
    private ArrayList<HashMap<String, Object>> list;  
    // 用来控制CheckBox的选中状况  
    private static HashMap<Integer,Boolean> isSelected;  
    // 上下文  
    private Context context;  
    // 用来导入布局  
    private LayoutInflater inflater = null;  
      
    // 构造器  
    public MyAdapter(ArrayList<HashMap<String, Object>> list, Context context) {  
        this.context = context;  
        this.list = list;  
        inflater = LayoutInflater.from(context);  
        isSelected = new HashMap<Integer, Boolean>();  
        // 初始化数据  
        initDate();  
    }  
    
    public void refresh(ArrayList<HashMap<String, Object>> list){
    	this.list = list;
    	initDate();
    	notifyDataSetChanged();
    }
  
    // 初始化isSelected的数据  
    private void initDate(){
        for(int i=0; i<list.size();i++) {  
            getIsSelected().put(i,false);  
        }  
    }  
  
    @Override  
    public int getCount() {  
        return list.size();  
    }  
  
    @Override  
    public Object getItem(int position) {  
        return list.get(position);  
    }  
  
    @Override  
    public long getItemId(int position) {  
        return position;  
    }  
  
    @Override  
    public View getView(int position, View convertView, ViewGroup parent) {  
        ViewHolder holder = null;  
        if (convertView == null) {  
            // 获得ViewHolder对象  
            holder = new ViewHolder();  
                // 导入布局并赋值给convertview  
            convertView = inflater.inflate(R.layout.list, null); 
            holder.img = (ImageView) convertView.findViewById(R.id.img);
            holder.tv = (TextView) convertView.findViewById(R.id.tv);  
            holder.cb = (CheckBox) convertView.findViewById(R.id.cb);  
            // 为view设置标签  
            convertView.setTag(holder);  
        } else {  
            // 取出holder  
            holder = (ViewHolder) convertView.getTag();  
            }  
        holder.img.setImageDrawable((Drawable) list.get(position).get("icon"));
        // 设置list中TextView的显示  
        holder.tv.setText(list.get(position).get("appName").toString());  
        // 根据isSelected来设置checkbox的选中状况  
        holder.cb.setChecked(getIsSelected().get(position));  
        return convertView;  
    }  
  
    public static HashMap<Integer,Boolean> getIsSelected() {  
        return isSelected;  
    }  
  
    public static void setIsSelected(HashMap<Integer,Boolean> isSelected) {  
    	MyAdapter.isSelected = isSelected;  
    }  

}  