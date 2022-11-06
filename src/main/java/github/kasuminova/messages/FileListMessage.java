package github.kasuminova.messages;

import github.kasuminova.balloonserver.utils.fileobject.AbstractSimpleFileObject;

public class FileListMessage extends AbstractMessage {
    AbstractSimpleFileObject[] fileObjects;

    public FileListMessage(AbstractSimpleFileObject[] fileObjects) {
        this.fileObjects = fileObjects;
    }

    public AbstractSimpleFileObject[] getFileObjects() {
        return fileObjects;
    }

    public void setFileObjects(AbstractSimpleFileObject[] fileObjects) {
        this.fileObjects = fileObjects;
    }
}
