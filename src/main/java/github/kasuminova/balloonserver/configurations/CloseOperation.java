package github.kasuminova.balloonserver.configurations;

public class CloseOperation {
    private final int operation;
    private String desc;

    public CloseOperation(int operation, String desc) {
        this.operation = operation;
        this.desc = desc;
    }

    public int getOperation() {
        return operation;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return desc;
    }
}
