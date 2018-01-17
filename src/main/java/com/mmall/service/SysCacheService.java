package com.mmall.service;

import com.google.common.base.Joiner;
import com.mmall.beans.CacheKeyConstants;
import com.mmall.common.RedisPool;
import com.mmall.util.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.ShardedJedis;

import javax.annotation.Resource;

@Service
@Slf4j
public class SysCacheService {

    @Resource(name = "redisPool")
    private RedisPool redisPool;

    /*
    * 为了适配Redis的API而封装的类
    * */
    public void saveCache(String toSavedValue, int timeoutSeconds, CacheKeyConstants prefix){
        saveCache(toSavedValue,timeoutSeconds,prefix,null);
    }
    public void saveCache(String toSavedValue, int timeoutSeconds, CacheKeyConstants prefix,String... keys){
        if (toSavedValue == null){
            return;
        }
        ShardedJedis shardedJedis = null;
        try{
                String cacheKey = generateCacheKey(prefix,keys);
                shardedJedis = redisPool.instance();
                shardedJedis.setex(cacheKey,timeoutSeconds,toSavedValue);
        }catch (Exception e){
                log.error("save cache exception, prefix:{}, key:{}",prefix.name(), JsonMapper.obj2String(keys));
        }finally {
            redisPool.safeClose(shardedJedis);
        }
    }
    private String generateCacheKey(CacheKeyConstants prefix, String... keys){
        String key = prefix.name();
        if (keys != null && keys.length > 0){
            key +="_" + Joiner.on("_").join(keys);
        }
        return key;
    }

}
