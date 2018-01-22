package com.mmall.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import javax.annotation.Resource;

@Service("redisPool")
@Slf4j
public class RedisPool {

    //被Spring管理的单例的service
    @Resource(name = "shardedJedisPool")
    private ShardedJedisPool shardedJedisPool;

    public ShardedJedis instance(){
        return shardedJedisPool.getResource();
    }

    public void safeClose(ShardedJedis shardedJedis){
            try{
                if (shardedJedis!= null){
                    shardedJedis.close();
                    }
            }catch(Exception e){
                log.error("return redis  resource exception", e);
            }
        }


}
