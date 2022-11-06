package github.kasuminova.messages;

import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;

public class AuthSuccessMessage extends AbstractMessage {
    private String clientID;
    private IntegratedServerConfig config;

    public AuthSuccessMessage(String clientID, IntegratedServerConfig config) {
        this.clientID = clientID;
        this.config = config;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public IntegratedServerConfig getConfig() {
        return config;
    }

    public void setConfig(IntegratedServerConfig config) {
        this.config = config;
    }
}
