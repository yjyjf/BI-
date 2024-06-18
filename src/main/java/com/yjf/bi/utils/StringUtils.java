package com.yjf.bi.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;

import java.io.IOException;

/**
 * 验证传入的字符串参数是否符合严格的JSON格式要求
 */
public class StringUtils {

    public static boolean isValidStrictly(String json) {
        if(org.apache.commons.lang3.StringUtils.isBlank(json)){
            return false;
        }
        TypeAdapter<JsonElement> strictAdapter = new Gson().getAdapter(JsonElement.class);
        try {
            //尝试使用获取到的TypeAdapter将输入的json字符串解析成一个JsonElement对象
            //如果传入的字符串能够成功被解析，那么可以认为它符合JSON格式
            strictAdapter.fromJson(json);
        } catch (JsonSyntaxException | IOException e) {
            return false;
        }
        return true;
    }
}


