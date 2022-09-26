package io.ml.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;

public class ProxyServerMain {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    }
}
