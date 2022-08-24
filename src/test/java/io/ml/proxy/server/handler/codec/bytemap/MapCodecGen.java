package io.ml.proxy.server.handler.codec.bytemap;

import java.util.*;

public class MapCodecGen {
    public static void main(String[] args) {
        List<Byte> byteList = new LinkedList<>();
        for(byte b = Byte.MIN_VALUE; b < Byte.MAX_VALUE; b ++) {
            byteList.add(b);
        }

        Random random = new Random();
        Map<Byte, Byte> byteMap = new HashMap<>();
        for(byte b = Byte.MIN_VALUE; b < Byte.MAX_VALUE; b ++) {
            Byte value = byteList.remove(random.nextInt(byteList.size()));
            byteMap.put(b, value);
            System.out.println(b + "=" + value);
        }

        System.out.println(byteMap);
    }
}
