package io.ml.proxy.server.handler.http;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpRequestInfo {
    private HttpRequest httpRequest;
    private boolean isSsl;
    private String remoteHost;
    private Integer remotePort;

    public HttpRequestInfo(HttpRequest request) {
        this.httpRequest = request;
        String uri = request.uri();
        String host = uri;
        if(host.contains("://")) {
            host = host.substring(host.indexOf("://") + 3);
        }
        if(host.contains("/")) {
            host = host.substring(0, host.indexOf("/"));
        }

        int port = 80;
        if (uri.startsWith("https://")) {
            port = 443;
        } else if(host.contains(":")) {
            port = Integer.parseInt(host.split(":")[1]);
            host = host.split(":")[0];
        }

        this.remoteHost = host;
        this.remotePort = port;
        this.isSsl = (request.method() == HttpMethod.CONNECT && port == 443) || uri.startsWith("https://");
    }
}
