package github.kasuminova.balloonserver.utils.fileobject;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.ArrayList;

public final class SimpleDirectoryObject extends AbstractSimpleFileObject {
    @JSONField(ordinal = 1)
    ArrayList<AbstractSimpleFileObject> children;

    public SimpleDirectoryObject(String name, ArrayList<AbstractSimpleFileObject> children) {
        this.name = name;
        this.children = children;
    }

    public ArrayList<AbstractSimpleFileObject> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<AbstractSimpleFileObject> children) {
        this.children = children;
    }
}
