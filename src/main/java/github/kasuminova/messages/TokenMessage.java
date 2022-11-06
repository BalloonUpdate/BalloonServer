package github.kasuminova.messages;

import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;

public class TokenMessage extends AbstractMessage {
    private String token;
    private ApplicationVersion clientVersion;

    public TokenMessage(String token, ApplicationVersion clientVersion) {
        this.token = token;
        this.clientVersion = clientVersion;
    }

    public ApplicationVersion getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(ApplicationVersion clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
