package github.kasuminova.balloonserver.httpserver;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import static github.kasuminova.balloonserver.BalloonServer.GLOBAL_LOGGER;

/**
 * 用于加载 SSL 证书的类（网上抄的）
 *
 * @author Kasumi_Nova
 */
public class SslContextFactoryOne {
    private static final String PROTOCOL = "TLS";

    /**
     * 服务器安全套接字协议
     */
    private static SSLContext serverContext = null;

    // 使用KeyTool生成密钥库和密钥时配置的密码

    public static SSLContext getServerContext(InputStream jks, char[] pass) {
        if (serverContext != null) {
            return serverContext;
        }
        try {
            //密钥库KeyStore
            KeyStore ks = KeyStore.getInstance("JKS");
            //加载服务端证书
            //加载服务端的 KeyStore, KeyStore 是生成仓库时设置的密码，用于检查密钥库完整性的密码
            ks.load(jks, pass);

            //密钥管理器
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            //初始化密钥管理器
            kmf.init(ks, pass);
            //获取安全套接字协议（TLS协议）的对象
            serverContext = SSLContext.getInstance(PROTOCOL);
            //初始化此上下文
            //参数一：认证的密钥  参数二：对等信任认证  参数三：伪随机数生成器。由于单向认证，服务端不用验证客户端，所以第二个参数为 null
            serverContext.init(kmf.getKeyManagers(), null, null);
        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        } finally {
            try {
                jks.close();
            } catch (IOException e) {
                GLOBAL_LOGGER.error(e);
            }
        }
        return serverContext;
    }
}
