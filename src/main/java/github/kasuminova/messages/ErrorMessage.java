package github.kasuminova.messages;

public class ErrorMessage extends AbstractMessage {
    String message;
    String stackTrace;

    public ErrorMessage(String message) {
        this(message, "");
    }

    public ErrorMessage(String message, String stackTrace) {
        this.message = message;
        this.stackTrace = stackTrace;
        this.messageType = "ErrorMessage";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
