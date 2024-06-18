package com.yjf.bi.utils;

import cn.hutool.extra.mail.MailUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EmailUtils {
    public static void sendCaptcha(String email,String captcha){
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = currentDate.format(dateTimeFormatter);
        MailUtil.send(email,"用户注册验证码",
                "本次请求的验证码为：\n"+
                "\n"+
                captcha+"\n"+
                "请在十分钟内完成验证\n"+
                "\n"+
                date,false
                );
    }

}
