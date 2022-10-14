package github.kasuminova.balloonserver.configurations;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * 配置文件抽象类
 */
abstract class Configuration {
    @JSONField(ordinal = 100)
    public int configVersion;

    public int getConfigVersion() {
        return configVersion;
    }

    public abstract Configuration setConfigVersion(int configVersion);
}
