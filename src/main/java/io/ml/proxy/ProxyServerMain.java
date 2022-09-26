package io.ml.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ml.proxy.config.properties.ProxyServerProperties;
import io.ml.proxy.config.properties.RelayProperties;
import io.ml.proxy.server.ProxyServer;
import io.ml.proxy.server.config.*;
import io.ml.proxy.utils.io.FileUtils;
import io.ml.proxy.utils.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.type.ArrayType;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ProxyServerMain {
    public static void main(String[] args) throws IOException {
        // 1. 加载配置
        // List<ProxyServerProperties> proxyServerPropertiesList = loadProxyServerProperties();
        File proxies = new File("proxies");
        if(!proxies.exists()) {
            System.out.println("代理配置目录不存在: " + proxies.getAbsolutePath());
            return;
        }
        List<File> fileList = null;
        {
            File[] files = proxies.listFiles();
            if (files != null) {
                fileList = Arrays.stream(files).filter(f -> f.getName().endsWith(".json")).collect(Collectors.toList());
            }
        }
        if(fileList == null || fileList.isEmpty()) {
            System.out.println("没有可用的代理配置: " + proxies.getAbsolutePath());
            return;
        }

        // 2. 读取配置
        List<ProxyServerProperties> proxyServerPropertiesList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, ProxyServerProperties.class);
        for(File file: fileList) {
            String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            List<ProxyServerProperties> proxyServerProperties = objectMapper.readValue(json, javaType);
            if(proxyServerProperties != null && !proxyServerProperties.isEmpty()) {
                proxyServerPropertiesList.addAll(proxyServerProperties);
            }
        }

        // 3. 配置服务器
        List<ProxyServerConfig> proxyServerConfigList = new ArrayList<>();
        for(ProxyServerProperties proxyServerProperties: proxyServerPropertiesList) {
            proxyServerConfigList.add(toProxyServerConfig(proxyServerProperties));
        }

        // 4. 启动服务器
        ProxyServer proxyServer = new ProxyServer();
        proxyServerConfigList.forEach(proxyServer::start);
    }

    private static List<ProxyServerProperties> loadProxyServerProperties() {
        List<ProxyServerProperties> proxyServerPropertiesList = new ArrayList<>();
        ProxyServerProperties proxyServerProperties = new ProxyServerProperties();
        // proxyServerProperties.setProxyProtocols();
        // proxyServerProperties.setHost();
        proxyServerProperties.setPort(40002);
        // proxyServerProperties.setUsernamePasswordAuth();

        List<RelayProperties> relayPropertiesList = new ArrayList<>();
        {
            RelayProperties relayProperties = new RelayProperties();
            relayProperties.setUsernamePasswordAuth(new UsernamePasswordAuth("hl-1", "hl888888"));
            relayProperties.setRelayProtocol(ProxyProtocolEnum.SOCKS5);
            // relayProperties.setEncryptionProtocol();
            // relayProperties.setRelayNetAddress(new NetAddress("74.91.26.90", 40000));
            relayProperties.setRelayNetAddress(new NetAddress("104.227.13.36", 8595));
            relayProperties.setRelayUsernamePasswordAuth(new UsernamePasswordAuth("ynmfeirt", "gufle6psmz2w"));
            relayPropertiesList.add(relayProperties);
        }
        {
            RelayProperties relayProperties = new RelayProperties();
            relayProperties.setUsernamePasswordAuth(new UsernamePasswordAuth("hl-2", "hl888888"));
            relayProperties.setRelayProtocol(ProxyProtocolEnum.SOCKS5);
            // relayProperties.setEncryptionProtocol();
            // relayProperties.setRelayNetAddress(new NetAddress("74.91.26.90", 40000));
            relayProperties.setRelayNetAddress(new NetAddress("45.72.53.174", 6210));
            relayProperties.setRelayUsernamePasswordAuth(new UsernamePasswordAuth("ynmfeirt", "gufle6psmz2w"));
            relayPropertiesList.add(relayProperties);
        }
        proxyServerProperties.setRelayPropertiesList(relayPropertiesList);

        proxyServerPropertiesList.add(proxyServerProperties);
        return proxyServerPropertiesList;
    }

    private static ProxyServerConfig toProxyServerConfig(ProxyServerProperties proxyServerProperties) throws UnknownHostException {

        // 配置代理服务器， 支持HTTP、HTTPS协议， 后续也会支持SOCKS5
        ProxyServerConfig proxyServerConfig = new ProxyServerConfig();
        proxyServerConfig.setProxyProtocols(proxyServerProperties.getProxyProtocols());
        proxyServerConfig.setCodecMsg(false);
        if(StringUtils.isNotBlack(proxyServerProperties.getHost())) {
            proxyServerConfig.setLocalAddress(new InetSocketAddress(InetAddress.getByName(proxyServerProperties.getHost()), proxyServerProperties.getPort()));
        } else {
            proxyServerConfig.setPort(proxyServerProperties.getPort());
        }
        proxyServerConfig.setBossGroupThreads(5);
        proxyServerConfig.setWorkerGroupThreads(10);

        List<UsernamePasswordAuth> usernamePasswordAuths = new ArrayList<>();
        usernamePasswordAuths.add(proxyServerProperties.getUsernamePasswordAuth());

        List<RelayProperties> relayPropertiesList = proxyServerProperties.getRelayPropertiesList();
        if(relayPropertiesList != null && !relayPropertiesList.isEmpty()) {
            Map<UsernamePasswordAuth, RelayConfig> relayConfigMap = new HashMap<>();
            proxyServerConfig.setRelayConfigMap(relayConfigMap);

            relayPropertiesList.forEach(relayProperties -> {
                // 配置真实代理服务器， 中继到SOCKS5服务
                RelayConfig relayConfig = new RelayConfig();
                relayConfig.setRelayProtocol(relayProperties.getRelayProtocol());
                relayConfig.setRelayNetAddress(relayProperties.getRelayNetAddress());
                relayConfig.setRelayUsernamePasswordAuth(relayProperties.getRelayUsernamePasswordAuth());

                ReplayRuleConfig replayRuleConfig = new ReplayRuleConfig();
                // replayRuleConfig.setDirectHosts(Arrays.asList("weixin", "qq", "tencent", "alibaba", "aliyun", "microsoft", "baidu", "hao123"));
                relayConfig.setReplayRuleConfig(replayRuleConfig);
                relayConfigMap.put(relayProperties.getUsernamePasswordAuth(), relayConfig);

                usernamePasswordAuths.add(relayProperties.getUsernamePasswordAuth());
            });
        }

        proxyServerConfig.setUsernamePasswordAuths(usernamePasswordAuths.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return proxyServerConfig;
    }
}
