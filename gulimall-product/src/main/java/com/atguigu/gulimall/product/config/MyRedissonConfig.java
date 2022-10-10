package com.atguigu.gulimall.product.config;

import org.mybatis.spring.annotation.MapperScan;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
//@MapperScan
public class MyRedissonConfig{

    @Bean
    public RedissonClient redisson() throws IOException {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.56.10:6379");
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

}
