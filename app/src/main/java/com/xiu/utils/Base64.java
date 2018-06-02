package com.xiu.utils;

import com.xiu.encoder.BASE64Decoder;
import com.xiu.encoder.BASE64Encoder;

/**
 * Created by xiu on 2017/12/25.
 */

public class Base64 {
    //将 s 进行 BASE64 编码
    public static String getBASE64(String s) {
        if (s == null) return null;
        return (new BASE64Encoder()).encode(s.getBytes());
    }

    //将 BASE64 编码的字符串 s 进行解码
    public static String getFromBASE64(String s) {
        if (s == null) return null;
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            byte[] b = decoder.decodeBuffer(s);
            return new String(b);
        } catch (Exception e) {
            return null;
        }
    }
}
