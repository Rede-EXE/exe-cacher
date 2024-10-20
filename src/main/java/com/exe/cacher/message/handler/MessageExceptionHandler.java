package com.exe.cacher.message.handler;

public class MessageExceptionHandler {

    public void onException(Exception exception) {
        System.out.println("Failed to send a message");
        exception.printStackTrace();
    }
}
