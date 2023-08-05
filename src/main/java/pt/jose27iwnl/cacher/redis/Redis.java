package pt.jose27iwnl.cacher.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Redis implements Closeable {

    public JedisPool jedisPool;

    public void load(RedisCredentials credentials) {
        try {
            String password;

            if (credentials.shouldAuthenticate()) {
                password = credentials.getPassword();
            } else {
                password = null;
            }

            jedisPool = new JedisPool(new JedisPoolConfig(), credentials.getHost(), credentials.getPort(), 5000, password, credentials.getDbId());
        } catch (Exception exception) {
            Logger.getGlobal().log(Level.WARNING, "Couldn't connect to Redis instance at " + credentials.getHost() + ":" + credentials.getPort(), exception);
        }
    }

    public <T> T runRedisCommand(Function<Jedis, T> lambda) {
        if (jedisPool == null || jedisPool.isClosed()) {
            throw new IllegalStateException("Jedis pool couldn't connect or is closed");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return lambda.apply(jedis);
        } catch (JedisException e) {
            throw new RuntimeException("Could not use resource and return", e);
        }
    }


    @Override
    public void close() throws IOException {
        if (jedisPool != null &&  !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
