package github.kasuminova.balloonserver.configurations;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * 配置文件抽象类
 */
abstract class Configuration {
    public static final int DEFAULT_PORT = 8080;

    @JSONField(ordinal = 100)
    public int configVersion;

    public int getConfigVersion() {
        return configVersion;
    }

    /**
     * 设置配置文件版本
     * @param configVersion 版本
     * @return Configuration
     */
    public abstract Configuration setConfigVersion(int configVersion);
}
