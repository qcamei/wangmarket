package com.xnx3.wangmarket.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;
import com.xnx3.j2ee.Global;
import com.xnx3.j2ee.func.Log;
import com.xnx3.net.HttpResponse;
import com.xnx3.net.HttpUtil;
import com.xnx3.wangmarket.admin.entity.Template;
import com.xnx3.wangmarket.admin.util.TemplateUtil;

/**
 * 云端模版更新使用线程。每间隔1天同步一次云端模版库的最新模版
 * @author 管雷鸣
 */
@Component
public class CloudTemplateUpdateThread {
	public static void main(String[] args) {
		new CloudTemplateUpdateThread();
	}

	public CloudTemplateUpdateThread() {
		new Thread(new Runnable() {
			public void run() {
				Log.info("Start the cloud template thread synchronization");
				HttpUtil http = new HttpUtil(HttpUtil.UTF8);
				http.setTimeout(5);//超时时间5秒
				
				boolean waitLoadDBFinish = true;	//等待数据库加载
				while(waitLoadDBFinish){
					if(Global.get("USER_REG_ROLE") != null){
						//有值了，已经加载完数据库信息了
						waitLoadDBFinish = false;
						Log.info("监测到已经加载数据库信息，2秒后同步云端模版");
					}
					try {
						//延迟2秒
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				
				//从云端加载模板
				//G.cloudTemplateMap = new HashMap<String, String>();
				
				while(true){
					boolean result = false;	//同步结果，为true则表示同步成功。如果不成功则继续同步
					HttpResponse hr = http.get("http://cloud.wscso.com/templatelist?v="+G.VERSION);
					if(hr.getCode() - 200 == 0){
						//取到新数据了
						if(hr.getContent() != null){
							JSONObject json = null;
							try {
								//使用try，避免cdn出现问题，终止此模版更新线程
								json = JSONObject.fromObject(hr.getContent());
							} catch (Exception e) {
								Log.error(e.getMessage());
								e.printStackTrace();
							}
							
							
							if(json != null){
								if(json.get("result") != null){
									if(json.getString("result").equals("1")){
										//获取云模版成功，同步到系统中。
										JSONArray jsonArray = json.getJSONArray("list");
										if(jsonArray == null){
											//应该不存在
											continue;
										}
										if(jsonArray.size() > 0 && TemplateUtil.cloudTemplateMap != null){
											//有值，那么先清空原本存储的，再填充入
											TemplateUtil.cloudTemplateMap.clear();
										}
										//便利最新的，再加入
										for (int i = 0; i < jsonArray.size(); i++) {
											JSONObject tempJson = jsonArray.getJSONObject(i);
											Template template = new Template();
											template.setAddtime(jsonGetInt(tempJson, "addtime"));
											template.setCompanyname(jsonGetString(tempJson, "companyname"));
											template.setName(jsonGetString(tempJson, "name"));
											template.setPreviewUrl(jsonGetString(tempJson, "previewUrl"));
											template.setRank(jsonGetInt(tempJson, "rank"));
											template.setRemark(jsonGetString(tempJson, "remark"));
											template.setSiteurl(jsonGetString(tempJson, "siteurl"));
											template.setTerminalDisplay((short) jsonGetInt(tempJson, "terminalDisplay"));
											template.setTerminalIpad((short) jsonGetInt(tempJson, "terminalIpad"));
											template.setTerminalMobile((short) jsonGetInt(tempJson, "terminalMobile"));
											template.setTerminalPc((short) jsonGetInt(tempJson, "terminalPc"));
											template.setType(jsonGetInt(tempJson, "type"));
											template.setUsername(jsonGetString(tempJson, "username"));
											template.setPreviewPic(jsonGetString(tempJson, "previewPic"));
											template.setWscsoDownUrl(jsonGetString(tempJson, "wscsoTemplateDown"));
											template.setZipDownUrl(jsonGetString(tempJson, "zipTemplateDown"));
	
											if(TemplateUtil.cloudTemplateMap == null){
												TemplateUtil.cloudTemplateMap = new HashMap<Integer, List<Template>>();
											}
											if(TemplateUtil.cloudTemplateMap.get(template.getType()) == null){
												TemplateUtil.cloudTemplateMap.put(template.getType(), new ArrayList<Template>());
											}
											TemplateUtil.cloudTemplateMap.get(template.getType()).add(template);
											
											//G.cloudTemplateMap.put(temp.getString("name"), temp.getString("intro"));
										}
										Log.info("同步云模版库完成，共同步"+jsonArray.size()+"个CMS模板");
										result = true;
									}
								}else{
									Log.error("非正常现象。同步云端模版库失败，接收json.result为null");
								}
							}else{
								Log.error("非正常现象。同步云端模版库失败，接收json为null");
							}
						}
					}
					
					
					try {
						if(result){
							//同步成功，则向后间隔1小时后再同步  1000 * 60 * 60 ＝ 3600000
							Thread.sleep(3600000);
						}else{
							Log.info("template cloud 同步失败，等待重试");
							//同步失败，则间隔3秒后再同步
							Thread.sleep(3000);
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			}
		}).start();
	}
	
	/**
	 * 获取 json 中的 int 值。若没有或空指针，返回0
	 * @param json 要取值的json对象
	 * @param key 要取的值的key
	 * @return
	 */
	public int jsonGetInt(JSONObject json, String key){
		if(json.get(key) == null){
			return 0;
		}
		try {
			return json.getInt(key);
		} catch (Exception e) {
			return 0;
		}
	}
	

	/**
	 * 获取 json 中的 String 值。若没有或空指针，返回""空字符串
	 * @param json 要取值的json对象
	 * @param key 要取的值的key
	 * @return
	 */
	public String jsonGetString(JSONObject json, String key){
		if(json.get(key) == null){
			return "";
		}
		try {
			return json.getString(key);
		} catch (Exception e) {
			return "";
		}
	}
}
