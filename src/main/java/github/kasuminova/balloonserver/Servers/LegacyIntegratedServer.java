package github.kasuminova.balloonserver.Servers;

import github.kasuminova.balloonserver.Utils.HashCalculator;

/**
 * 旧版 IntegratedServer 面板实例，支持多实例化，兼容 4.x.x 版本的客户端
 * @author Kasumi_Nova
 */
public final class LegacyIntegratedServer extends AbstractIntegratedServer {
    public LegacyIntegratedServer(String serverName, boolean autoStart) {
        super(serverName, autoStart, ".legacy.lscfg.json");

        this.resJsonFileExtensionName = "legacy_res-cache";
        this.hashAlgorithm = HashCalculator.SHA1;
    }
}
