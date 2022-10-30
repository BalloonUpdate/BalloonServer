package github.kasuminova.messages;

public class LogMessage extends AbstractMessage {
    private String message;
    private String level;

    public LogMessage(String level, String message) {
        this.level = level;
        this.message = message;
        this.messageType = LogMessage.class.getName();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
