package pt.jose27iwnl.cacher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import lombok.Getter;
import lombok.Setter;
import pt.jose27iwnl.cacher.message.Message;
import pt.jose27iwnl.cacher.message.handler.IncomingMessageHandler;
import pt.jose27iwnl.cacher.message.handler.MessageExceptionHandler;
import pt.jose27iwnl.cacher.message.listener.MessageListener;
import pt.jose27iwnl.cacher.message.listener.MessageListenerData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cacher implements Closeable {
    private final String channel;
    private final JedisPool jedisPool;
    private final Gson gson;
    private final boolean async;

    private JedisPubSub jedisPubSub;

    private final Map<String, List<MessageListenerData>> listeners = new HashMap<>();

    public Cacher(String channel, JedisPool jedisPool, Gson gson, boolean async) {
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.gson = gson;
        this.async = async;

        setup();
    }

    private void setup() {
        jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equalsIgnoreCase(Cacher.this.channel) && message != null) {
                    try {
                        int breakAt = message.indexOf(';');
                        if (breakAt < 0) {
                            Logger.getGlobal().severe("[Cacher] Invalid message format: " + message);
                            return; // or throw an exception if necessary
                        }

                        String messageId = message.substring(0, breakAt);

                        if (listeners.containsKey(messageId)) {
                            JsonObject messageData = gson.fromJson(message.substring(breakAt + 1), JsonObject.class);

                            for (MessageListenerData listener : listeners.get(messageId)) {
                                listener.getMethod().invoke(listener.getInstance(), messageData);
                            }
                        }
                    } catch (JsonParseException exception) {
                        Logger.getGlobal().log(Level.SEVERE, "[Cacher] Failed to parse message into JSON", exception);
                    } catch (Exception exception) {
                        Logger.getGlobal().log(Level.SEVERE, "[Cacher] Failed to handle message", exception);
                    }
                }
            }
        };

        if (async) {
            ForkJoinPool.commonPool().execute(() -> {
                try(Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(jedisPubSub, channel);
                }
            });
        } else {
            try(Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(jedisPubSub, channel);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            jedisPubSub.unsubscribe();
        }

        jedisPool.close();
    }


    public void sendMessage(Message message) {
        sendMessage(message, null);
    }

    public void sendMessage(Message message, MessageExceptionHandler exceptionHandler) {
        try (Jedis jedis = jedisPool.getResource()) {
            String jsonData = gson.toJsonTree(message.getData()).toString();
            String messageContent = message.getId() + ";" + jsonData;
            jedis.publish(channel, messageContent);
        } catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.onException(e);
            }
        }
    }

    public void registerListener(MessageListener messageListener) {
        Class<? extends MessageListener> listenerClass = messageListener.getClass();
        for (Method method : listenerClass.getDeclaredMethods()) {
            IncomingMessageHandler annotation = method.getAnnotation(IncomingMessageHandler.class);
            if (annotation != null && method.getParameterCount() > 0) {
                Class<?> firstParameterType = method.getParameterTypes()[0];
                if (!JsonObject.class.isAssignableFrom(firstParameterType)) {
                    throw new IllegalStateException("First parameter should be of JsonObject type");
                }

                String messageId = annotation.id();
                listeners.putIfAbsent(messageId, new ArrayList<>());
                listeners.get(messageId).add(new MessageListenerData(messageListener, method, messageId));
            }
        }
    }


}
