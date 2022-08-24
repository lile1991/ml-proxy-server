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
     * @param classPathname 类路径下的代理配置， 如 proxy/us.txt"
     * @return
     */
    private static synchronized List<Proxy> initProxies(String classPathname) {
        if(!PROXY_LIST_MAP.containsKey(classPathname)) {
            URL resource = ProxyConfigList.class.getResource(classPathname);
            if(resource == null) {
                return null;
            }

            File socks5File = new File(resource.getFile());
            if(socks5File.exists()) {
                try (InputStream resourceAsStream = new FileInputStream(socks5File)) {
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
                    PROXY_LIST_MAP.put(classPathname, proxies);
                    return proxies;
                } catch (IOException e) {
                    log.error("Initial proxy IP exception", e);
                    PROXY_LIST_MAP.put(classPathname, Collections.emptyList());
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     *
     * @param classPathname 类路径下的代理配置， 如 proxy/us.txt"
     */
    public static List<Proxy> getProxies(String classPathname) {
        List<Proxy> proxies = PROXY_LIST_MAP.get(classPathname);
        if(proxies == null) {
            proxies = initProxies(classPathname);
        }
        return new ArrayList<>(proxies);
    }
}
