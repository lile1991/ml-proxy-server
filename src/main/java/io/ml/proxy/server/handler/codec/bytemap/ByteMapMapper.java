package io.ml.proxy.server.handler.codec.bytemap;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ByteMapMapper {
    private final static Map<Byte, Byte> encodeMap;
    private final static Map<Byte, Byte> decodeMap;

    static {
        encodeMap = new HashMap<>();
        decodeMap = new HashMap<>();
        File file = new File("codec/map.txt");
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = bufferedReader.readLine()) != null) {
                String[] split = line.split("=");
                byte b1 = Byte.parseByte(split[0]);
                byte b2 = Byte.parseByte(split[1]);
                encodeMap.put(b1, b2);
                decodeMap.put(b2, b1);
            }
        } catch (FileNotFoundException e) {
            log.error("MapCodec initialize failure! The map.txt file not found!", e);
        } catch (IOException e) {
            log.error("MapCodec initialize failure! Reader the map.txt file error!", e);
        }
    }

    public static byte encode(byte value) {
        return encodeMap.get(value);
    }

    public static byte decode(byte value) {
        return decodeMap.get(value);
    }

}
