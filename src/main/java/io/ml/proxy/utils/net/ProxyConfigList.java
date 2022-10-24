package io.ml.proxy.utils.net;

import io.ml.proxy.server.config.ProxyProtocolEnum;
import io.ml.proxy.utils.io.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ProxyConfigList {
    public static final Map<String, List<Proxy>> PROXY_LIST_MAP = new HashMap<>();

    /**
     * @param path 类路径下的代理配置， 如 proxy/us.txt"
     */
    private static synchronized List<Proxy> initProxies(String path) {
        if(!PROXY_LIST_MAP.containsKey(path)) {
            File proxyFile;

            if(path.startsWith("classpath:")) {
                String classpath = path.substring("classpath:".length());
                URL resource = ProxyConfigList.class.getResource(classpath);
                if(resource == null) {
                    return null;
                }
                proxyFile = new File(resource.getFile());
            } else {
                proxyFile = new File(path);
            }

            if(proxyFile.exists()) {
                try (InputStream resourceAsStream = new FileInputStream(proxyFile)) {
                    List<String> socks5ProxyList = IOUtils.readLines(resourceAsStream, "UTF-8");
                    List<Proxy> proxies = socks5ProxyList.stream()
                            .map(ProxyConfigList::parseProxyStr)
                            .collect(Collectors.toList());
                    PROXY_LIST_MAP.put(path, proxies);
                    return proxies;
                } catch (IOException e) {
                    log.error("Initial proxy IP exception", e);
                    PROXY_LIST_MAP.put(path, Collections.emptyList());
                }
            }
        }
        return Collections.emptyList();
    }

    public static Proxy parseProxyStr(String proxyStr) {
        // HTTP:proxy-1:hl888888@74.91.26.90:40000
        // 1. 先解协议
        int idx = proxyStr.indexOf(":");
        String protocol = proxyStr.substring(0, idx);
        proxyStr = proxyStr.substring(idx + 1);

        String username = "";
        String password = "";

        // 2. 解用户名密码
        // proxy-1:hl888888@74.91.26.90:40000
        // proxy-1:@74.91.26.90:40000
        // 74.91.26.90:40000
        int upIdx = proxyStr.indexOf("@");
        if(upIdx != -1) {
            String usernamePassword = proxyStr.substring(0, upIdx);
            // 有用户名密码
            String[] up = usernamePassword.split(":", -1);
            username = up[0];
            if(up.length > 1) {
                password = up[1];
            }

            proxyStr = proxyStr.substring(upIdx + 1);
        }

        // 3. 解代理主机
        // 74.91.26.90:40000
        String[] split = proxyStr.split(":");
        String hostname = split[0];
        int port = Integer.parseInt(split[1]);

        return new Proxy(ProxyProtocolEnum.valueOf(protocol), null, hostname, port, username, password);
    }

    /**
     * 支持使用类路径
     * @param path 如 proxy/us100.txt 或 classpath:/proxy/us100.txt
     */
    public static List<Proxy> getProxies(String path) {
        List<Proxy> proxies = PROXY_LIST_MAP.get(path);
        if(proxies == null) {
            proxies = initProxies(path);
        }
        return proxies;
    }
}
