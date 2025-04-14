package com.syncfusion.redis;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;


@Configuration
public class RedisConfig {
    @Bean
    RedisConnectionFactory connectionFactory(Environment env) {
        String isClusterMode = env.getProperty("redis.isClusterEnabled","true");
        String password = env.getProperty("redis.password");
        String redisNodes = env.getProperty("redis.nodes");
        String[] redisUri = redisNodes.split(",");
        JedisConnectionFactory connectionFactory = null;
        if(isClusterMode.equalsIgnoreCase("false")) {
            connectionFactory = new JedisConnectionFactory(new RedisStandaloneConfiguration(redisUri[0].split(":")[0], Integer.parseInt(redisUri[0].split(":")[1]) ));
        }
        else {
            connectionFactory = new JedisConnectionFactory(new RedisClusterConfiguration(Arrays.asList(redisUri)));
        }
        if (StringUtils.isNotBlank(password)){
            connectionFactory.setPassword(password);
        }
        return connectionFactory;
    }
    private JedisPoolConfig getJedisPoolConfig(Environment env){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(Integer.parseInt(env.getProperty("redis.maxTotalConnections")));
        jedisPoolConfig.setMaxIdle(Integer.parseInt(env.getProperty("redis.maxIdleConnections")));
        jedisPoolConfig.setMaxWaitMillis(Long.parseLong(env.getProperty("redis.maxWaitMillis")));
        jedisPoolConfig.setTestOnBorrow(Boolean.parseBoolean(env.getProperty("redis.isTestOnBorrow")));
        return jedisPoolConfig;
    }
}
