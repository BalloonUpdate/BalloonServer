package github.kasuminova.balloonserver.utils.fileobject;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

public final class SimpleDirectoryObject extends AbstractSimpleFileObject {
    @JSONField(ordinal = 1)
    List<AbstractSimpleFileObject> children;

    public SimpleDirectoryObject(String name, List<AbstractSimpleFileObject> children) {
        this.name = name;
        this.children = children;
    }

    public List<AbstractSimpleFileObject> getChildren() {
        return children;
    }

    public void setChildren(List<AbstractSimpleFileObject> children) {
        this.children = children;
    }
}