package com.authguard.usersystem.util; // 确保包名与你的项目结构一致

import jakarta.servlet.http.HttpServletRequest; // 使用 jakarta.servlet
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils; // Spring Framework 工具类

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpUtils {

    private static final Logger logger = LoggerFactory.getLogger(IpUtils.class);

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final int IP_ADDRESS_LENGTH = 15; // IPv4地址的最大长度

    /**
     * 获取客户端真实IP地址
     * <p>
     * 考虑了常见的代理情况: X-Forwarded-For, Proxy-Client-IP, WL-Proxy-Client-IP, HTTP_CLIENT_IP, HTTP_X_FORWARDED_FOR
     * 以及 request.getRemoteAddr() 作为最后的回退。
     * </p>
     * @param request HttpServletRequest 对象
     * @return 客户端IP地址，如果获取失败则返回 "unknown"
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ipAddress = null;
        try {
            // 1. X-Forwarded-For: 经过多个代理时，第一个非 unknown 的IP通常是真实客户端IP
            ipAddress = request.getHeader("X-Forwarded-For");
            if (isValidIp(ipAddress)) {
                // 对于 X-Forwarded-For，它可能包含多个IP，第一个通常是客户端真实IP
                if (ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
                if (isValidIp(ipAddress)) return ipAddress;
            }

            // 2. Proxy-Client-IP
            ipAddress = request.getHeader("Proxy-Client-IP");
            if (isValidIp(ipAddress)) {
                return ipAddress;
            }

            // 3. WL-Proxy-Client-IP (WebLogic)
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
            if (isValidIp(ipAddress)) {
                return ipAddress;
            }

            // 4. HTTP_CLIENT_IP (一些代理)
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
            if (isValidIp(ipAddress)) {
                return ipAddress;
            }

            // 5. HTTP_X_FORWARDED_FOR (一些代理)
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
            if (isValidIp(ipAddress)) {
                // 对于 HTTP_X_FORWARDED_FOR，它可能包含多个IP，第一个通常是客户端真实IP
                if (ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
                if (isValidIp(ipAddress)) return ipAddress;
            }

            // 6. X-Real-IP (Nginx等代理)
            ipAddress = request.getHeader("X-Real-IP");
            if (isValidIp(ipAddress)) {
                return ipAddress;
            }

            // 7. request.getRemoteAddr()
            ipAddress = request.getRemoteAddr();
            if (LOCALHOST_IPV4.equals(ipAddress) || LOCALHOST_IPV6.equals(ipAddress)) {
                // 根据网卡取本机配置的IP
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ipAddress = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    logger.warn("Failed to get local host address: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred while getting IP address: {}", e.getMessage(), e);
            ipAddress = UNKNOWN; // 发生异常时，安全起见返回 unknown
        }

        // 最后的长度校验和处理，确保IP地址不会过长
        if (ipAddress != null && ipAddress.length() > IP_ADDRESS_LENGTH && ipAddress.contains(",")) {
            ipAddress = ipAddress.substring(0, ipAddress.indexOf(",")).trim();
        }

        return StringUtils.hasText(ipAddress) ? ipAddress : UNKNOWN;
    }

    /**
     * 简单校验IP地址是否有效（非空、非"unknown"）
     * @param ipAddress IP地址字符串
     * @return 如果有效返回 true，否则 false
     */
    private static boolean isValidIp(String ipAddress) {
        return StringUtils.hasText(ipAddress) && !UNKNOWN.equalsIgnoreCase(ipAddress.trim());
    }

}
