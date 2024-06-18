package com.yjf.bi.utils;

import cn.hutool.extra.mail.MailUtil;
import org.junit.jupiter.api.Test;

class EmailUtilsTest {

    @Test
    void sendCaptcha() {
        MailUtil.send("2965583335@qq.com", "测试", "邮件来自Hutool测试", false);
    }
}