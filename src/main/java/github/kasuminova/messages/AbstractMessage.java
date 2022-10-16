package github.kasuminova.messages;

import java.io.Serial;
import java.io.Serializable;

public abstract class AbstractMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    protected String messageType = AbstractMessage.class.getName();

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
