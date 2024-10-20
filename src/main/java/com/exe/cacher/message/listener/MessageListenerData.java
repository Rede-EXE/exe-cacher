package com.exe.cacher.message.listener;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;

@Data
@AllArgsConstructor
public class MessageListenerData {
    private Object instance;
    private Method method;
    private String id;
}
