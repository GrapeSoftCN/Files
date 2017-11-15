package Model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.dbFilter;

public class CommonModel {
	private static AtomicInteger fileNO = new AtomicInteger(0);
	private JSONObject configString = null;
	
	public CommonModel() {
		configString = appsProxy.configValue();
		if (configString != null) {
			configString = JSONObject.toJSON(configString.getString("other"));
		}
	}
	
	/**
	 * 整合参数，将JSONObject类型的参数封装成JSONArray类型
	 * 
	 * @param object
	 * @return
	 */
	public JSONArray buildCond(String Info) {
		String key;
		Object value;
		JSONArray condArray = null;
		JSONObject object = JSONObject.toJSON(Info);
		dbFilter filter = new dbFilter();
		if (object != null && object.size() > 0) {
			for (Object object2 : object.keySet()) {
				key = object2.toString();
				value = object.get(key);
				filter.eq(key, value);
			}
			condArray = filter.build();
		}else{
		    condArray = JSONArray.toJSONArray(Info);
		}
		return condArray;
	}

	
	public String getUnqueue() {
		return (new Integer(fileNO.incrementAndGet())).toString();
	}
	
	/**
	 * 获取文件存储绝对路径
	 * 
	 * @project GrapeFile
	 * @package model
	 * @file UploadModel.java
	 * 
	 * @param path
	 * @return
	 *
	 */
	public String getImagepath(String path) {
		int i = 0;
		if (path.contains("http://")) {
			if (path.contains("File//upload")) {
				i = path.toLowerCase().indexOf("file//upload");
				path = "\\" + path.substring(i);
			}
			if (path.contains("File\\upload")) {
				i = path.toLowerCase().indexOf("file\\upload");
				path = "\\" + path.substring(i);
			}
			if (path.contains("File/upload")) {
				i = path.toLowerCase().indexOf("file/upload");
				path = "\\" + path.substring(i);
			}
		}
		return path;
	}
	
	/**
	 * 获取configName字段中的配置信息
	 * @param key
	 * @return
	 */
	public String getFilePath(String key) {
		JSONObject object = null;
		String value = "";
		if (configString !=null && configString.size() > 0) {
			object = configString.getJson("upload");
		}
		if (object != null && object.size() > 0) {
		    if (object.containsKey(key)) {
		        value = object.getString(key);
            }
		}
		return value;
	}
	
	/**
     * 获取当前日期
     * 
     * @return 年-月-日
     */
    public String getDate() {
        Date currentTime = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = format.format(currentTime);
        return dateString;
    }
}
