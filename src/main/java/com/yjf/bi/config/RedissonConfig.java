package com.yjf.bi.config;


import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson的配置
 * @Author yjf
 */
@Configuration
//读取配置文件yml中 前缀为spring.redis的配置
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private String port;

    private String password;
    @Bean
    public RedissonClient redissonClient(){
        //创建配置
        Config config = new Config();
        //redis地址
        String redisAddress = String.format("redis://%s:%s", host, port);
        //添加单机Redisson的配置
        config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(1)
                .setPassword(password);
        //2.创建实例
        RedissonClient redisson= Redisson.create(config);
        return redisson;

    }


}
