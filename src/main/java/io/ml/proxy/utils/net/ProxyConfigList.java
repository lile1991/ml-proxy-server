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
    private static final Random RANDOM = new Random();

    /**
     * @param path 类路径下的代理配置， 如 proxy/us.txt"
     * @return
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
                    List<Proxy> proxies = socks5ProxyList.stream().map(socks5Proxy -> {
                        String[] split = socks5Proxy.split(":");
                        String protocol = split[0];
                        String hostname = split[1];
                        int port = Integer.parseInt(split[2]);
                        String username = "";
                        String password = "";
                        if(split.length > 3) {
                            username = split[3];
                        }
                        if(split.length > 4) {
                            password = split[4];
                        }

                        return new Proxy(ProxyProtocolEnum.valueOf(protocol), null, hostname, port, username, password);
                    }).collect(Collectors.toList());
                    PROXY_LIST_MAP.put(path, proxies);
                    return proxies;
                } catch (IOException e) {
                    log.error("Initial proxy IP exception", e);
                    PROXY_LIST_MAP.put(path, Collections.emptyList());
                }
            } else {
                log.warn("The file {} not exists!", proxyFile.getAbsolutePath());
            }
        }
        return Collections.emptyList();
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
        return new ArrayList<>(proxies);
    }
}
