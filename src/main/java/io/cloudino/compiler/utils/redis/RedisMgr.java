package io.cloudino.compiler.utils.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Created by serch on 7/27/15.
 */
public class RedisMgr {
    private static Logger logger = Logger.getLogger("1.c.r.RedisMgr");
    private static final HashMap<String, JedisPool> albercas = new HashMap<>();

    public static Jedis getJedis(final String endpointName){
        JedisPool pool = albercas.get(endpointName);
        if (null!=pool){
            return pool.getResource();
        } else return null;
    }

    public synchronized static boolean createJedis(final String endpointName, final String serverIP){
        if (!albercas.containsKey(endpointName)) {
            albercas.putIfAbsent(endpointName, new JedisPool(new JedisPoolConfig(), serverIP));
            return true;
        }
        return false;
    }

    public static boolean putByteArray(final String endpointName, final String key, final byte[]data){
        if (albercas.containsKey(endpointName)){
            try (Jedis jedis = getJedis(endpointName)){
                jedis.set(key.getBytes("utf-8"),data);
                return true;
            } catch (UnsupportedEncodingException e) {
                logger.warning("Can't set data to Redis:"+e.getLocalizedMessage());
            }
        }
        return false;
    }

    public static byte[] getByteArray(final String endpointName, final String key){
        if (albercas.containsKey(endpointName)){
            try (Jedis jedis = getJedis(endpointName)){
                byte[] data = jedis.get(key.getBytes("utf-8"));
                jedis.del(key.getBytes("utf-8"));
                return data;
            } catch (Exception e) {
                logger.warning("Error reteiving/deleting a Redis key: "+ e.getLocalizedMessage());
            }
        }
        return null;
    }
}
