package com.yjf.bi.manager;

import com.yjf.bi.common.ErrorCode;
import com.yjf.bi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RedisLimiterManager {
    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     * @param key 区分不同的限流器，不同的用户分别统计
     */
    public void doRateLimit(String key){
        //创建一个限流器,
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        //RateType.OVERALL：全局限流模式，意味着所有请求共享相同的限流规则。
        //2：在指定的时间间隔内，系统最多允许通过的操作数量为2。
        //1：这个数字定义了时间窗口的大小，单位为second，即表示时间窗口为1秒
        //RateIntervalUnit.SECONDS：速率的时间单位，这里是秒。
        //总含义即为：每秒最多允许2个操作通过
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        //  如果没有令牌,还想执行操作,就抛出异常
        if(!canOp){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }

    }

}
