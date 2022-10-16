package github.kasuminova.messages.fileobject;

import github.kasuminova.messages.AbstractMessage;

public abstract class AbstractLiteFileObject extends AbstractMessage {
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
