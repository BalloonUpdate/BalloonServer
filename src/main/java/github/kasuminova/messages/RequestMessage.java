package github.kasuminova.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求信息
 */
public class RequestMessage extends AbstractMessage {
    /**
     * 消息类型
     */
    private String requestType;

    /**
     * 消息参数
     */
    private List<String> requestParams;

    /**
     * 新建一个请求信息
     * @param requestType 请求类型
     */
    public RequestMessage(String requestType, List<String> requestParams) {
        this.requestType = requestType;
        this.requestParams = requestParams;
        this.messageType = RequestMessage.class.getName();
    }

    public RequestMessage(String requestType) {
        this(requestType, new ArrayList<>());
    }

    public List<String> getRequestParams() {
        return requestParams;
    }

    public RequestMessage setRequestParams(List<String> requestParams) {
        this.requestParams = requestParams;
        return this;
    }

    public String getRequestType() {
        return requestType;
    }

    public RequestMessage setRequestType(String requestType) {
        this.requestType = requestType;
        return this;
    }
}
