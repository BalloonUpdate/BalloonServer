package github.kasuminova.messages.fileobject;

import java.util.ArrayList;

public final class LiteDirectoryObject extends AbstractLiteFileObject {
    ArrayList<AbstractLiteFileObject> children;

    public LiteDirectoryObject(String name, ArrayList<AbstractLiteFileObject> children) {
        this.name = name;
        this.children = children;
    }

    public ArrayList<AbstractLiteFileObject> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<AbstractLiteFileObject> children) {
        this.children = children;
    }
}
