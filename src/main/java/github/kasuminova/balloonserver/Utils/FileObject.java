package github.kasuminova.balloonserver.Utils;

import java.util.ArrayList;
import java.util.List;

public class FileObject {
    public static abstract class AbstractSimpleFileObject {
        String name;
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SimpleFileObject extends AbstractSimpleFileObject {
        public SimpleFileObject(String name, long length, String hash, long modified) {
            this.name = name;
            this.length = length;
            this.hash = hash;
            this.modified = modified;
        }
        long length;
        String hash;
        long modified;

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public long getModified() {
            return modified;
        }

        public void setModified(long modified) {
            this.modified = modified;
        }
    }

    public static class SimpleDirectoryObject extends AbstractSimpleFileObject {
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
}
