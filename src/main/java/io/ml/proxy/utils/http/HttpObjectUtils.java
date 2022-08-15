package io.ml.proxy.utils.http;

import io.ml.proxy.utils.GZIPUtils;
import io.ml.proxy.utils.io.IOUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpContent;
import lombok.extern.slf4j.Slf4j;
import org.brotli.dec.BrotliInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpObjectUtils {
    public static String stringOf(Object msg) {
        if(msg instanceof FullHttpMessage) {
            String response = msg.toString();
            FullHttpMessage fullHttpResponse = (FullHttpMessage) msg;
            String contentEncoding = fullHttpResponse.headers().get("content-encoding");
            String contentType = fullHttpResponse.headers().get("content-type");
            String content;
            if(contentType != null && contentType.startsWith("image")) {
                content = "Do not show";
            } else {
                content = decodeContent(fullHttpResponse.content(), getCharset(contentType), contentEncoding);
            }

            return response + "\n\n" + content;
        } else if(msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            return httpContent.content().toString(StandardCharsets.UTF_8);
        } else if(msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            return byteBuf.toString(StandardCharsets.UTF_8);
        }
        return msg.toString();
    }

    private static String decodeContent(ByteBuf content, String charset, String contentEncoding) {
        // return content.toString(Charset.forName(charset));
        try {
            if("gzip".equalsIgnoreCase(contentEncoding)) {
                byte[] bytes = new byte[content.readableBytes()];
                content.copy().readBytes(bytes);
                return GZIPUtils.uncompress(bytes);
            } else if ("br".equalsIgnoreCase(contentEncoding)) {
                byte[] bytes = new byte[content.readableBytes()];
                content.copy().readBytes(bytes);
                try (BrotliInputStream bis = new BrotliInputStream(new ByteArrayInputStream(bytes))) {
                    return IOUtils.toString(bis, charset);
                }
            }
            return content.toString(Charset.forName(charset));
        } catch (IOException e) {
            log.error("Failed to decode content", e);
            return "Failed to decode content: " + e.getMessage();
        }
    }

    public static String getCharset(String contentType) {
        if(contentType == null) {
            return "UTF-8";
        }
        contentType = contentType.toLowerCase();
        if(contentType.contains("utf-8")) {
            return "UTF-8";
        }
        if(contentType.contains("gbk")) {
            return "GBK";
        }
        if(contentType.contains("gb2312")) {
            return "GB2312";
        }
        return "UTF-8";
    }
}
