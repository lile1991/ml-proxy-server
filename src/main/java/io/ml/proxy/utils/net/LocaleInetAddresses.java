package io.ml.proxy.utils.net;

import io.ml.proxy.utils.io.IOUtils;
import io.ml.proxy.utils.lang.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
public class LocaleInetAddresses {

    @Getter
    private static InetAddress[] inetAddresses;

    static {
        File file = new File("ip_addresses.txt");
        if(file.exists()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                String ips = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                inetAddresses = Arrays.stream(ips.split("\\s+")).map(String::trim).filter(StringUtils::isNotBlack).map(ip -> {
                    try {
                        return InetAddress.getByName(ip);
                    } catch (UnknownHostException e) {
                        log.error("ip=" + ip, e);
                    }
                    return null;
                }).filter(Objects::nonNull).toArray(InetAddress[]::new);
                log.debug("Successfully loaded into IP list: {}", Arrays.asList(inetAddresses));
            } catch (Exception e) {
                log.error("IP configuration not read, use default IP: {}", e.getMessage());
            }
        }
    }

    public static InetAddress next() {
        return inetAddresses == null ? null : inetAddresses[(int) (System.currentTimeMillis() % inetAddresses.length)];
    }
}
