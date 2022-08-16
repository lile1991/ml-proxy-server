package io.ml.proxy.utils.net;

import io.ml.proxy.server.config.ProxyProtocolEnum;
import io.ml.proxy.utils.io.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class ProxyConfigList {
    public static final List<Proxy> PROXY_LIST = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        URL resource = ProxyConfigList.class.getResource("/proxy/socks5_us.txt");
        if(resource != null) {
            try (InputStream resourceAsStream = new FileInputStream(resource.getFile())) {
                List<String> socks5ProxyList = IOUtils.readLines(resourceAsStream, "UTF-8");
                socks5ProxyList.forEach(socks5Proxy -> {
                    String[] split = socks5Proxy.split(":");
                    String hostname = split[0];
                    int port = Integer.parseInt(split[1]);
                    String username = split[2];
                    String password = split[3];

                    Proxy proxy = new Proxy(ProxyProtocolEnum.SOCKS5, hostname, port, username, password);
                    PROXY_LIST.add(proxy);
                });
            } catch (IOException e) {
                log.error("初始化代理IP异常", e);
            }
        }
    }

    public static Proxy randomProxy() {
        return PROXY_LIST.get(RANDOM.nextInt(PROXY_LIST.size() - 1));
    }
    public static List<Proxy> getProxies() {
        return PROXY_LIST;
    }
}
