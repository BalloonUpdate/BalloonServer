package github.kasuminova.messages;

@Deprecated
public class MethodMessage extends AbstractMessage {
    private String className;
    private String methodName;
    private String[] params;

    public MethodMessage(String className, String methodName) {
        this(className, methodName, null);
    }

    public MethodMessage(String className, String methodName, String[] methods) {
        this.className = className;
        this.methodName = methodName;
        this.params = methods;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String[] getParams() {
        return params;
    }

    public MethodMessage setParams(String[] params) {
        this.params = params;
        return this;
    }
}
