package github.kasuminova.messages;

public class TokenMessage extends AbstractMessage {
    private String token;
    private String clientVersion;

    public TokenMessage(String token, String clientVersion) {
        this.token = token;
        this.clientVersion = clientVersion;

        messageType = TokenMessage.class.getName();
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
