package github.kasuminova.messages;

public class StringMessage extends AbstractMessage {
    String message;

    public StringMessage() {
        this.messageType = "StringMessage";
    }

    public StringMessage(String message) {
        this();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
