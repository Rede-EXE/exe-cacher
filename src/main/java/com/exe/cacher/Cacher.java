package com.exe.cacher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.exe.cacher.message.Message;
import com.exe.cacher.message.handler.IncomingMessageHandler;
import com.exe.cacher.message.handler.MessageExceptionHandler;
import com.exe.cacher.message.listener.MessageListener;
import com.exe.cacher.message.listener.MessageListenerData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cacher implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(Cacher.class.getName());

    private final String channel;
    private final JedisPool jedisPool;
    private final Gson gson;
    private final boolean async;
    private final JedisPubSub jedisPubSub;
    private final Map<String, List<MessageListenerData>> listeners = new ConcurrentHashMap<>();

    public Cacher(String channel, JedisPool jedisPool, Gson gson, boolean async) {
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.gson = gson;
        this.async = async;
        this.jedisPubSub = createJedisPubSub();

        setupSubscription();
    }

    private JedisPubSub createJedisPubSub() {
        return new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handleMessage(channel, message);
            }
        };
    }

    private void setupSubscription() {
        Runnable subscriptionTask = () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(jedisPubSub, channel);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to set up subscription", e);
            }
        };

        if (async) {
            CompletableFuture.runAsync(subscriptionTask);
        } else {
            subscriptionTask.run();
        }
    }

    private void handleMessage(String channel, String message) {
        if (!this.channel.equalsIgnoreCase(channel) || message == null) {
            return;
        }

        try {
            int breakAt = message.indexOf(';');
            if (breakAt < 0) {
                LOGGER.severe("Invalid message format: " + message);
                return;
            }

            String messageId = message.substring(0, breakAt);
            List<MessageListenerData> messageListeners = listeners.get(messageId);

            if (messageListeners != null) {
                JsonObject messageData = gson.fromJson(message.substring(breakAt + 1), JsonObject.class);
                for (MessageListenerData listener : messageListeners) {
                    invokeListener(listener, messageData);
                }
            }
        } catch (JsonParseException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse message into JSON", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to handle message", e);
        }
    }

    private void invokeListener(MessageListenerData listener, JsonObject messageData) {
        try {
            listener.getMethod().invoke(listener.getInstance(), messageData);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to invoke listener", e);
        }
    }

    @Override
    public void close() {
        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            jedisPubSub.unsubscribe();
        }
        jedisPool.close();
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
            if (annotation != null && method.getParameterCount() > 0 && JsonObject.class.isAssignableFrom(method.getParameterTypes()[0])) {
                String messageId = annotation.id();
                listeners.computeIfAbsent(messageId, k -> new ArrayList<>())
                        .add(new MessageListenerData(messageListener, method, messageId));
            }
        }
    }
}