package com.yjf.bi.retry;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MyRetryListener implements RetryListener {

    @Override
    public <v> void  onRetry(Attempt<v> attempt){
        log.info("重试次数：{}",attempt.getAttemptNumber());
        log.info("延迟时间：{}",attempt.getDelaySinceFirstAttempt()+ "毫秒");
        if (attempt.hasException()){
            log.info("异常信息：{}",attempt.getExceptionCause().getMessage());
        }
        if (attempt.hasResult()){
            log.info("重试结果：{}",attempt.getResult());
        }
    }


}
