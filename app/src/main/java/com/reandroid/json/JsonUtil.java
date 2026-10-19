/*
 * Copyright (c) 2002 JSON.org (now "Public Domain")
 * This is NOT property of REAndroid
 * This package is renamed from org.json.* to avoid class conflict when used on android platforms
*/
package com.reandroid.json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import android.util.Base64;

public class JsonUtil {

    public static byte[] parseBase64(String text){
        if(text == null || !text.startsWith(JSONItem.MIME_BIN_BASE64)){
            return null;
        }
        text = text.substring(JSONItem.MIME_BIN_BASE64.length());
        try{
            return Base64.decode(text, Base64.URL_SAFE | Base64.NO_WRAP);
        }catch (Throwable throwable){
            throw new JSONException(throwable);
        }
    }
    public static void readJSONObject(File file, JSONConvert<JSONObject> jsonConvert) throws IOException {
        FileInputStream inputStream=new FileInputStream(file);
        readJSONObject(inputStream, jsonConvert);
        inputStream.close();
    }
    public static void readJSONObject(InputStream inputStream, JSONConvert<JSONObject> jsonConvert){
        InputStreamReader reader=new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        readJSONObject(reader, jsonConvert);
    }
    public static void readJSONObject(Reader reader, JSONConvert<JSONObject> jsonConvert){
        JSONObject jsonObject=new JSONObject(new JSONTokener(reader));
        jsonConvert.fromJson(jsonObject);
    }

    public static void readJSONArray(File file, JSONConvert<JSONArray> jsonConvert) throws IOException {
        FileInputStream inputStream=new FileInputStream(file);
        readJSONArray(inputStream, jsonConvert);
        inputStream.close();
    }
    public static void readJSONArray(InputStream inputStream, JSONConvert<JSONArray> jsonConvert){
        InputStreamReader reader=new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        readJSONArray(reader, jsonConvert);
    }
    public static void readJSONArray(Reader reader, JSONConvert<JSONArray> jsonConvert){
        JSONArray jsonObject=new JSONArray(new JSONTokener(reader));
        jsonConvert.fromJson(jsonObject);
    }

}
