package github.kasuminova.messages.filemessages;

public class FileInfoMsg extends FileMessage {
    private long size;

    public FileInfoMsg(String filePath, String fileName, long size) {
        super(filePath, fileName);
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
