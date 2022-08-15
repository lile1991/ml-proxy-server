package io.ml.client.socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

public class NioSocketClient {

    public static void main0(String[] args) throws Exception {
        int port = 443;
        String host = "ckadmin.grights.club";
        String path = "/hw-facebook-admin/pubIpInfo/";
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
        socketChannel.configureBlocking(false);

        SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(null, null, null);
        SSLEngine sslEngine = sslCtx.createSSLEngine(host, port);
        sslEngine.setUseClientMode(true);
        sslEngine.beginHandshake();
        SSLSession sslSession = sslEngine.getSession();
        SSLEngineResult.HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
        ByteBuffer appBuffer = ByteBuffer.allocate(sslSession.getApplicationBufferSize());
        ByteBuffer packetBuffer = ByteBuffer.allocate(sslSession.getPacketBufferSize());

        ByteBuffer appWBuffer = ByteBuffer.allocate(sslSession.getApplicationBufferSize());
        ByteBuffer packetWBuffer = ByteBuffer.allocate(sslSession.getPacketBufferSize());

        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED && handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    socketChannel.read(packetBuffer);
                    packetBuffer.flip();
                    SSLEngineResult res = sslEngine.unwrap(packetBuffer, appBuffer);
                    packetBuffer.compact();
                    handshakeStatus = res.getHandshakeStatus();
                    break;
                case NEED_WRAP:
                    packetWBuffer.clear();
                    res = sslEngine.wrap(appWBuffer, packetWBuffer);
                    handshakeStatus = res.getHandshakeStatus();
                    if (res.getStatus() == SSLEngineResult.Status.OK) {
                        packetWBuffer.flip();
                        while (packetWBuffer.hasRemaining()) {
                            socketChannel.write(packetWBuffer);
                        }
                    }
                    break;
                case NEED_TASK:
                    Runnable task;
                    while ((task = sslEngine.getDelegatedTask()) != null) {
                        new Thread(task).start();
                    }
                    handshakeStatus = sslEngine.getHandshakeStatus();
                    break;
            }
        }

        String getCmd = "GET " + path + " HTTP/1.1 \r\n" + "Host: " + host + "\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "\r\n";
        ByteBuffer byteBuffer = ByteBuffer.wrap(getCmd.getBytes());

        packetBuffer.clear();
        SSLEngineResult res = sslEngine.wrap(byteBuffer, packetBuffer);
        if (res.getStatus() != SSLEngineResult.Status.OK) {
            throw new RuntimeException("SSL加密失败");
        }
        packetBuffer.flip();

        while (packetBuffer.hasRemaining()) {
            socketChannel.write(packetBuffer);
        }

        int num;
        byte[] body = null;
        int bodyIndex = 0;
        int headerIndex = 0;
        int contentLength = 5120;
        byte[] originHeader = new byte[1024];
        LinkedHashMap<String, List<String>> head = null;
        appBuffer.clear();
        packetBuffer.clear();
        while ((num = socketChannel.read(packetBuffer)) > -2) {
            packetBuffer.flip();
            do {
                res = sslEngine.unwrap(packetBuffer, appBuffer);
            } while (res.getStatus() == SSLEngineResult.Status.OK);
            packetBuffer.compact();
            for (int i = 0; i < appBuffer.position(); i++) {
                byte b = appBuffer.get(i);
                if (head == null) {
                    originHeader[headerIndex++] = b;
                    if (originHeader.length == headerIndex) {
                        originHeader = byteExpansion(originHeader, 1024);
                    }
                    if (originHeader[headerIndex - 1] == '\n' && originHeader[headerIndex - 2] == '\r' && originHeader[headerIndex - 3] == '\n' && originHeader[headerIndex - 4] == '\r') {
                        String headerStr = new String(originHeader);
                        String[] headerList = headerStr.split("\r\n");
                        head = Arrays.stream(headerList).skip(1).filter(h -> h.contains(":")).collect(Collectors.groupingBy(h -> h.split(":")[0].trim(), LinkedHashMap::new, Collectors.mapping(h -> h.split(":")[1].trim(), Collectors.toList())));
                        contentLength = Optional.ofNullable(head.get("Content-Length")).map(c -> Integer.parseInt(c.get(0))).orElse(-1);
                    }
                } else {
                    Integer finalContentLength = contentLength;
                    body = Optional.ofNullable(body).orElseGet(() -> new byte[finalContentLength]);
                    body[bodyIndex++] = b;
                    if (bodyIndex == contentLength) {
                        num = -2;
                        break;
                    }
                }
            }
            if (num < 0) {
                socketChannel.close();
                System.out.println(new String(originHeader));
                System.out.println(new String(uncompress(body)));
                return;
            }
            appBuffer.clear();
        }
    }

    /**
     * Byte array expansion
     */
    public static byte[] byteExpansion(byte[] origin, int num) {
        return Optional.ofNullable(origin).map(o -> {
            byte[] temp = new byte[o.length + num];
            IntStream.range(0, o.length).forEach(i -> temp[i] = o[i]);
            return temp;
        }).orElseGet(() -> new byte[num]);
    }

    public static byte[] uncompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream gzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

}
