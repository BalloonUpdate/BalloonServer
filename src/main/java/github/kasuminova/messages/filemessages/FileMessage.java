package github.kasuminova.messages.filemessages;

import github.kasuminova.messages.AbstractMessage;

import java.io.Serial;

public abstract class FileMessage extends AbstractMessage {
    @Serial
    private static final long serialVersionUID = 1L;
    String filePath;
    String fileName;

    public FileMessage(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
