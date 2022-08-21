package io.ml.proxy.utils.net;

import io.ml.proxy.server.config.ProxyProtocolEnum;
import io.ml.proxy.utils.io.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class ProxyConfigList {
    public static final List<Proxy> PROXY_LIST = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        File socks5File = new File("proxy/socks5_us.txt");
        if(socks5File.exists()) {
            try (InputStream resourceAsStream = new FileInputStream(socks5File)) {
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
                log.error("Initial proxy IP exception", e);
            }
        }
    }

    public static List<Proxy> getProxies() {
        return new ArrayList<>(PROXY_LIST);
    }
}
