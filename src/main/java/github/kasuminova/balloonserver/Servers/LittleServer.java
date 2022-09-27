package github.kasuminova.balloonserver.Servers;

import github.kasuminova.balloonserver.Utils.HashCalculator;

/**
 * LittleServer 面板实例，支持多实例化，兼容 4.1.14 以上的客户端
 * @author Kasumi_Nova
 */
public class LittleServer extends AbstractLittleServer {
    public LittleServer(String serverName, boolean autoStart) {
        super(serverName, autoStart);
        this.hashAlgorithm = HashCalculator.CRC32;
    }
}
