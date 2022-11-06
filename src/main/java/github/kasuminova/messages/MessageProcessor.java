package github.kasuminova.messages;

public interface MessageProcessor<T> {
    void process(T message0);
}
