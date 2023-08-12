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

    public JedisPool localJedisPool;
    public JedisPool backboneJedisPool;

    public void load(RedisCredentials localCredentials, RedisCredentials backboneCredentials) {
        try {
            String password;

            if (localCredentials.shouldAuthenticate()) {
                password = localCredentials.getPassword();
            } else {
                password = null;
            }

            localJedisPool = new JedisPool(new JedisPoolConfig(), localCredentials.getHost(), localCredentials.getPort(), 5000, password, localCredentials.getDbId());
        } catch (Exception exception) {
            Logger.getGlobal().log(Level.WARNING, "Couldn't connect to Local Redis instance at " + localCredentials.getHost() + ":" + localCredentials.getPort(), exception);
        }

        try {
            String password;

            if (backboneCredentials.shouldAuthenticate()) {
                password = backboneCredentials.getPassword();
            } else {
                password = null;
            }

            backboneJedisPool = new JedisPool(new JedisPoolConfig(), backboneCredentials.getHost(), backboneCredentials.getPort(), 5000, password, backboneCredentials.getDbId());
        } catch (Exception exception) {
            Logger.getGlobal().log(Level.WARNING, "Couldn't connect to Backbone Redis instance at " + backboneCredentials.getHost() + ":" + backboneCredentials.getPort(), exception);
        }
    }

    /**
     * A functional method for using a pooled [Jedis] resource and returning data.
     *
     * @param lambda the function
     */
    public <T> T runRedisCommand(Function<Jedis, T> lambda) {
        if (localJedisPool == null || localJedisPool.isClosed()) {
            throw new IllegalStateException("Jedis pool couldn't connect or is closed");
        }

        try (Jedis jedis = localJedisPool.getResource()) {
            return lambda.apply(jedis);
        } catch (JedisException e) {
            throw new RuntimeException("Could not use resource and return", e);
        }
    }

    /**
     * A functional method for using a pooled [Jedis] resource and returning data.
     *
     * @param lambda the function
     */
    public <T> T runBackboneRedisCommand(Function<Jedis, T> lambda) {
        if (backboneJedisPool == null || backboneJedisPool.isClosed()) {
            throw new IllegalStateException("Jedis pool couldn't connect or is closed");
        }

        try (Jedis jedis = backboneJedisPool.getResource()) {
            return lambda.apply(jedis);
        } catch (JedisException e) {
            throw new RuntimeException("Could not use resource and return", e);
        }
    }


    @Override
    public void close() throws IOException {
        if (localJedisPool != null && !localJedisPool.isClosed()) {
            localJedisPool.close();
        }

        if (backboneJedisPool != null && !backboneJedisPool.isClosed()) {
            backboneJedisPool.close();
        }
    }
}
