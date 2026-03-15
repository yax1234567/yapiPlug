package org.yax.yapiplug.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.yax.yapiplug.http.OkHttpUtil;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class YapiUtil {
    private static Logger log = Logger.getLogger("YapiUtil");
   private static String yapiUrl="http://yapi.orsd.tech/";
    private static String exepath=System.getProperty("exe.path");
    private static String exeAddress=System.getProperty("exe4j.moduleName");

    public static String getExepath() {
        return exepath;
    }

    public static String getYapiUrl() {
        return yapiUrl;
    }

    public static void setYapiUrl(String yapiUrl) {
        YapiUtil.yapiUrl = yapiUrl;
    }

    public static JSONObject addYapi(String username, String cookie, String project, String menu, String path, String title, String resBody, List<Map<String,Object>> queryMapList,String markdown, Map<String,String> keyValueMap){
            Map map = new HashMap();
            map.put("catid", menu);
            map.put("project_id", project);
            map.put("req_body_is_json_schema", true);
            map.put("res_body_is_json_schema", true);
            map.put("res_body_type", "json");
            map.put("edit_uid", 0);
            map.put("status", "undone");
            map.put("type", "static");
            map.put("method", "GET");
            map.put("desc","<p>"+markdown+"</p>");
            map.put("markdown",markdown);
            map.put("switch_notice",true);
            map.put("path", path.contains("/")?path:"/" + path + "/1.0.0/action");
            map.put("title", title);
            map.put("req_query", queryMapList);
            map.put("username", username);
            if(resBody!=null&&!"".equals(resBody)) {
                String jsonSchema = jsonTojsonSchema(resBody,keyValueMap);
                map.put("res_body", jsonSchema);
            }
            return createApi(cookie, map);
   }
    public static  Map<String,String> loginYapi(String email,String password){
        HashMap<String, String > param=new HashMap<>();
        param.put("email",email);
        param.put("password",password);
        Map<String,String> result=OkHttpUtil.postForCookie(yapiUrl+"/api/user/login",param);
        return result;
    }
   public static JSONObject listGroup(String cookie){
        return JSONObject.parseObject(OkHttpUtil.get(yapiUrl+"/api/group/list",cookie,null));
   }
   public static JSONObject listProject(String cookie,String group_id){
       HashMap<String, String > param=new HashMap<>();
       param.put("group_id",group_id);
       return JSONObject.parseObject(OkHttpUtil.get(yapiUrl+"/api/project/list",cookie,param));
    }
    public static JSONObject listMenu(String cookie,String project_id){
        HashMap<String, String > param=new HashMap<>();
        param.put("project_id",project_id);
        return JSONObject.parseObject(OkHttpUtil.get(yapiUrl+"/api/interface/list_menu",cookie,param));
    }
    public static JSONObject listCat(String cookie,String cat_id){
        HashMap<String, String > param=new HashMap<>();
        param.put("catid",cat_id);
        param.put("page","1");
        param.put("limit","1000");
        return JSONObject.parseObject(OkHttpUtil.get(yapiUrl+"/api/interface/list_cat",cookie,param));
    }
    public static JSONObject getApi(String cookie,String id){
        HashMap<String, String > param=new HashMap<>();
        param.put("id",id);
        return JSONObject.parseObject(OkHttpUtil.get(yapiUrl+"/api/interface/get",cookie,param));
    }
    public static JSONObject transferYapiT(String cookie,String project_id,String catid,String id){
        Map<String,Object> param=new HashMap<>();
        param.put("project_id",project_id);
        param.put("catid",catid);
        param.put("id",id);
    return JSONObject.parseObject(OkHttpUtil.postJSON(yapiUrl+"/api/interface/up",cookie,param));
    }
    public static JSONObject createApi(String cookie,Map param){
        String response=OkHttpUtil.postJSON(yapiUrl+"/api/interface/add",cookie,param);
      return  JSONObject.parseObject(response);
    }
    public static JSONObject saveApi(String cookie,Map param){
        String response=OkHttpUtil.postJSON(yapiUrl+"/api/interface/save",cookie,param);
        return  JSONObject.parseObject(response);
    }
    public static JSONObject updateApi(String cookie,Map param){
        String response=OkHttpUtil.postJSON(yapiUrl+"/api/interface/up",cookie,param);
        return  JSONObject.parseObject(response);
    }
    public  static String jsonTojsonSchema(String json, Map<String,String> keyValueMap){
        JSONObject jsonObject=JSONObject.parseObject(json, Feature.OrderedField);
        if(jsonObject!=null){ jsonObject.put("success","1"); }
        recursion(jsonObject,keyValueMap);
        JSONObject result=new JSONObject();
        result.put("$schema","http://json-schema.org/draft-04/schema#");
        result.put("type","object");
        result.put("properties",jsonObject);
        return result.toString();
    }
    public static void recursion(JSONObject jsonObject, Map<String,String> keyValueMap){
        Set<Map.Entry<String, Object>> set=jsonObject.entrySet();
        Iterator<Map.Entry<String, Object>> iterator=set.iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Object> entry=iterator.next();
            String key= entry.getKey();
            Object value=entry.getValue();
            if(value instanceof JSONObject){
                JSONObject temp=new JSONObject();
                temp.put("type","object");
                temp.put("properties",value);
                jsonObject.put(key,temp);
                recursion((JSONObject) value,keyValueMap);
                continue;
            }
            if(value instanceof List){
                JSONObject temp=new JSONObject();
                JSONObject itemTemp=new JSONObject();
                List<JSONObject> tempList= (List<JSONObject>) value;
                JSONObject json=tempList.get(0);
                temp.put("type","array");
                itemTemp.put("type","object");
                itemTemp.put("properties",json);
                temp.put("items",itemTemp);
                jsonObject.put(key,temp);
                recursion(json,keyValueMap);
                continue;
            }
            if(value instanceof Integer){
                JSONObject temp=new JSONObject();
                temp.put("type","integer");
                temp.put("description",value);
                jsonObject.put(key,temp);
                String valueTemp=keyValueMap.get(key);
                if(valueTemp!=null&&!"".equals(valueTemp)){
                    temp.put("description",valueTemp);
                }
                continue;
            }
            if(value instanceof String){
                JSONObject temp=new JSONObject();
                temp.put("type","string");
                temp.put("description",value);
                jsonObject.put(key,temp);
                String valueTemp=keyValueMap.get(key);
                if(valueTemp!=null&&!"".equals(valueTemp)){
                    temp.put("description",valueTemp);
                }
                continue;
            }
            if(value instanceof Number){
                JSONObject temp=new JSONObject();
                temp.put("type","number");
                temp.put("description",value);
                jsonObject.put(key,temp);
                String valueTemp=keyValueMap.get(key);
                if(valueTemp!=null&&!"".equals(valueTemp)){
                    temp.put("description",valueTemp);
                }
                continue;
            }
            if(value instanceof Boolean){
                JSONObject temp=new JSONObject();
                temp.put("type","boolean");
                temp.put("description",value);
                jsonObject.put(key,temp);
                String valueTemp=keyValueMap.get(key);
                if(valueTemp!=null&&!"".equals(valueTemp)){
                    temp.put("description",valueTemp);
                }
                continue;
            }
        }
    }
    private  static Map<String,String> result=new HashMap<>();
    public static Map<String,String> analysis(String source){
        result.clear();
        source=source.replace("comment","COMMENT");
        int index=0;
        while(index>=0){
            index=source.indexOf("COMMENT",index);
            if(index>=0){
                getKeyValue(source,index);
                index+=8;
            }
        }
        return result;
    }
    public static void getKeyValue(String source,int index){
        int length=source.length();
        int start=-1;
        int end=-1;
        for(int i=index+8;i<length;i++){
            char ch=source.charAt(i);
            if('\''==ch){
                if(start==-1) {
                    start = i;
                    continue;
                }
                end=i;
                break;
            }
        }
        if(end+1<length&&source.charAt(end+1)==';'){
           return;
        }
        String value=source.substring(start+1,end);
        end=-1;
        for(int i=index-1;i>=0;i--){
            char ch=source.charAt(i);
            if('`'==ch){
                if(end==-1) {
                    end = i;
                    continue;
                }
                start=i;
                break;
            }
        }
        String key=source.substring(start+1,end);
        result.put(key,value);
    }

}
