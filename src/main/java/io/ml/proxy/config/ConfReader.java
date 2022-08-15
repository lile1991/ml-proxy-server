package io.ml.proxy.config;

import io.ml.proxy.utils.io.FileUtils;
import io.ml.proxy.utils.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfReader {

    public static class PropertyDefined {
        String name;
        String configName;
        boolean isMultiLine;

        public PropertyDefined(String name) {
            this(name, false);
        }

        public PropertyDefined(String name, boolean isMultiLine) {
            this.name = name;
            this.configName = "[" + name + "]";
            this.isMultiLine = isMultiLine;
        }
    }

    public static Map<String, Object> readFile(File confFile, List<PropertyDefined> propertyDefinedList) throws IOException {
        if(!confFile.exists()) {
            return null;
        }

        Map<String, Object> properties = new HashMap<>();
        List<String> lines = FileUtils.readLines(confFile, StandardCharsets.UTF_8);

        String nameOfMultiProp = null;
        StringBuilder valueOfMultiProp = null;
        out: for(String line: lines) {
            for(PropertyDefined propertyDefined: propertyDefinedList) {
                if(line.startsWith(propertyDefined.configName)) {
                    if(propertyDefined.isMultiLine) {
                        nameOfMultiProp = propertyDefined.name;
                        valueOfMultiProp = new StringBuilder();
                        properties.put(nameOfMultiProp, valueOfMultiProp);
                        continue out;
                    }
                    nameOfMultiProp = null;
                    valueOfMultiProp = null;
                    continue out;
                }
            }

            if(nameOfMultiProp == null) {
                if(StringUtils.isBlack(line)) {
                    continue;
                }
                int eqIdx = line.indexOf("=");
                properties.put(line.substring(0, eqIdx), line.substring(eqIdx));
                continue;
            }

            valueOfMultiProp.append(line);
        }   // End for

        return properties;
    }
}
