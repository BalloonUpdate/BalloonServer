package github.kasuminova.balloonserver.Configurations;

public class CloseOperations {
    private final int operation;
    private String desc;

    public CloseOperations(int operation, String desc) {
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
