package github.kasuminova.balloonserver.configurations;

import com.alibaba.fastjson2.annotation.JSONField;

public class RemoteClientConfig extends Configuration {
    @JSONField(ordinal = 1)
    private String ip = "127.0.0.1";
    @JSONField(ordinal = 2)
    private int port = 8080;
    @JSONField(ordinal = 3)
    private String token = "";

    public RemoteClientConfig() {
        this.configVersion = 0;
    }

    @Override
    public RemoteClientConfig setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public RemoteClientConfig setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public int getPort() {
        return port;
    }

    public RemoteClientConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getToken() {
        return token;
    }

    public RemoteClientConfig setToken(String token) {
        this.token = token;
        return this;
    }
}
