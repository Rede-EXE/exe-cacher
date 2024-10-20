package com.exe.cacher.redis;

import lombok.Getter;

@Getter
public class RedisCredentials {
    private String host;
    private int port;
    private String password;
    private int dbId;

    public RedisCredentials() {
        this.host = "localhost";
        this.port = 6379;
        this.password = null;
        this.dbId = 0;
    }

    public RedisCredentials(String host, int port, String password, int dbId) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.dbId = dbId;
    }

    public boolean shouldAuthenticate() {
        return password != null && !password.isEmpty() && !password.trim().isEmpty();
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    public static class Builder {
        private final RedisCredentials credentials = new RedisCredentials();

        public Builder host(String host) {
            credentials.setHost(host);
            return this;
        }

        public Builder port(int port) {
            credentials.setPort(port);
            return this;
        }

        public Builder password(String password) {
            credentials.setPassword(password);
            return this;
        }

        public Builder dbId(int dbId) {
            credentials.setDbId(dbId);
            return this;
        }

        public RedisCredentials build() {
            return credentials;
        }
    }
}

