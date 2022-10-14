package github.kasuminova.balloonserver.Utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPAddressUtil {
    /**
     * 判断地址是 IPv4 还是 IPv6.
     *
     * @param address 要验证的 IP 地址
     * @return "v6" "v4" 如果两者都不是，返回 null
     */
    public static String checkAddress(String address) {
        try {
            InetAddress iNetAddress = InetAddress.getByName(address);
            if (iNetAddress instanceof Inet6Address) return "v6";
            if (iNetAddress instanceof Inet4Address) return "v4";
        } catch (UnknownHostException ignored) {}
        return null;
    }
}