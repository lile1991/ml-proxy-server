package io.ml.proxy.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPUtils {
    public static byte[] compress(String str) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        }
    }

    public static String uncompress(byte[] str) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(str))) {
            int b;
            while ((b = gis.read()) != -1) {
                baos.write((byte) b);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
