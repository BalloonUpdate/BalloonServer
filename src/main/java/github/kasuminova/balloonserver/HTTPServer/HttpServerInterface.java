package github.kasuminova.balloonserver.HTTPServer;

/**
 * HTTP 服务器的接口，用于控制启动状态
 * <p>TODO:更多功能待更新</p>
 */
public interface HttpServerInterface {

    /**
     * 启动服务器
     * @return 是否启动成功
     */
    boolean startServer();
}
