package com.xiu.utils;

import android.content.Context;
import android.util.Log;

import org.mozilla.javascript.NativeObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class JSSecret {
    private static Invocable inv;
    public static final String encText = "encText";
    public static final String encSecKey = "encSecKey";

    /**
     * 从本地加载修改后的 js 文件到 scriptEngine
     */
/*    static {
        try {
            Path path = Paths.get("core.js");
            byte[] bytes = Files.readAllBytes(path);
            String js = new String(bytes);
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            engine.eval(js);
            inv = (Invocable) engine;
            System.out.println("Init completed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public static void init(Context context){
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open("core.js"),"UTF-8");
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line;
            StringBuilder res= new StringBuilder();
            while((line = bufReader.readLine()) != null){
                res.append(line);
            }
            String js = res.toString();
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("rhino");
            engine.eval(js);
            inv = (Invocable) engine;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean invIsNull(){
        return inv == null;
    }

/*    public static ScriptObjectMirror get_params(String paras) throws Exception {
        inv.invokeFunction("myFunc", paras);
        return so;
    }*/

    public static HashMap<String, String> getDatas(String paras) {
        try {
            //Object object = inv.invokeFunction("myFunc", paras);
            NativeObject no = (NativeObject) inv.invokeFunction("myFunc", paras);

/*            Set<Map.Entry<Object, Object>> entries = no.entrySet();
            for (Map.Entry<Object,Object> tmp: entries) {
                Log.d("key:",tmp.getKey()+"");
                Log.d("value:",tmp.getValue()+"");
            }*/


            HashMap<String, String> datas = new HashMap<>();
            datas.put("params", no.get(JSSecret.encText).toString());
            datas.put("encSecKey", no.get(JSSecret.encSecKey).toString());
            return datas;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
