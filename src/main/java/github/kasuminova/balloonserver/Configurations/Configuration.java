package github.kasuminova.balloonserver.Configurations;

/**
 * 配置文件抽象类
 */
abstract class Configuration {
    public int configVersion;
    public int getConfigVersion() {
        return configVersion;
    }
    public abstract Configuration setConfigVersion(int configVersion);
}
