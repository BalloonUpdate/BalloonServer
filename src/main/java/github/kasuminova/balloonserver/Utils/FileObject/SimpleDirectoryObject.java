package github.kasuminova.balloonserver.Utils.FileObject;

import java.util.List;

public class SimpleDirectoryObject extends AbstractSimpleFileObject {
    public SimpleDirectoryObject(String name, List<AbstractSimpleFileObject> children) {
        this.name = name;
        this.children = children;
    }

    List<AbstractSimpleFileObject> children;

    public void setChildren(List<AbstractSimpleFileObject> children) {
        this.children = children;
    }

    public List<AbstractSimpleFileObject> getChildren() {
        return children;
    }
}
