package org.yax.yapiplug.http;

import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 文件名称： OkHttpUtil<br/>
 * 初始作者： 【yax】 <br/>
 * 创建日期： 2019/12/5 14:44<br/>
 * 功能说明：  <br/>
 * <br/>
 * ================================================<br/>
 * 修改记录：<br/>
 * 修改作者 日期 修改内容<br/>
 * <br/>
 * ================================================<br/>
 * Copyright 中仑网络科技有限公司 2019/12/5 .All rights reserved.<br/>
 */
public class OkHttpUtil {
    private static OkHttpClient okHttpClient = new OkHttpClient();
    private static Logger log = Logger.getLogger("OkHttpUtil");
    public static String  post(String url,String cookie, HashMap<String, String > paramsMap){
        long starttime=System.currentTimeMillis();
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if(paramsMap!=null) {
            Set<String> keySet = paramsMap.keySet();
            for (String key : keySet) {
                String value = paramsMap.get(key);
                formBodyBuilder.add(key, value);
            }
        }
        FormBody formBody = formBodyBuilder.build();
        Request request = new Request
                .Builder()
                .post(formBody)
                .url(url).header("Cookie", cookie)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            log.info("http耗时："+(System.currentTimeMillis()-starttime));
            return response.body().string();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public static String get(String url,String cookie, HashMap<String, String > paramsMap){
        long starttime=System.currentTimeMillis();
        if(paramsMap!=null&&!paramsMap.isEmpty()){
            StringBuilder sb=new StringBuilder();
            for(Map.Entry<String,String> entry:paramsMap.entrySet()){
                sb.append("&"+entry.getKey()+"="+entry.getValue());
            }
            url+="?"+sb.substring(1);
        }
        Request request = new Request.Builder().url(url).header("Cookie", cookie).get().build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            String string = response.body().string();
            log.info("http耗时："+(System.currentTimeMillis()-starttime));
            return string;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    public static Map<String,String>  postForCookie(String url, HashMap<String, String > paramsMap){
        long starttime=System.currentTimeMillis();
        Map<String,String> result=new HashMap<>();
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        Set<String> keySet = paramsMap.keySet();
        for(String key:keySet) {
            String value = paramsMap.get(key);
            formBodyBuilder.add(key,value);
        }
        FormBody formBody = formBodyBuilder.build();

        Request request = new Request
                .Builder()
                .post(formBody)
                .url(url)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            Headers headers = response.headers();
            HttpUrl loginUrl = request.url();
            List<Cookie> cookies = Cookie.parseAll(loginUrl, headers);
            StringBuilder cookieStr = new StringBuilder();
            for (Cookie cookieT : cookies) {
                cookieStr.append(cookieT.name()).append("=").append(cookieT.value() + ";");
            }
            result.put("Cookie",cookieStr.toString());
            result.put("response",response.body().string());
            log.info("http耗时："+(System.currentTimeMillis()-starttime));
            return result;
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    public static String  postCookie(String url,String cookie, HashMap<String, String > paramsMap){
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        Set<String> keySet = paramsMap.keySet();
        for(String key:keySet) {
            String value = paramsMap.get(key);
            formBodyBuilder.add(key,value);
        }
        FormBody formBody = formBodyBuilder.build();

        Request request = new Request
                .Builder()
                .post(formBody)
                .url(url).header("Cookie", cookie)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            Headers headers = response.headers();
            HttpUrl loginUrl = request.url();
            List<Cookie> cookies = Cookie.parseAll(loginUrl, headers);
            StringBuilder cookieStr = new StringBuilder();
            for (Cookie cookieT : cookies) {
                cookieStr.append(cookieT.name()).append("=").append(cookieT.value() + ";");
            }
            return cookieStr.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static String postJSON(String url,String cookie, Object json) {
        long starttime=System.currentTimeMillis();
        RequestBody body = RequestBody.create(JSON, JSONObject.toJSONString(json));
        Request request = new Request.Builder()
                .url(url)
                .header("Cookie", cookie)
                .post(body)
                .build();
        try {
        Response response = okHttpClient.newCall(request).execute();
            log.info("http耗时："+(System.currentTimeMillis()-starttime));
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
