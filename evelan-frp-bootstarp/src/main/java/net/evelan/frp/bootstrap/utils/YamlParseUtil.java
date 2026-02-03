package net.evelan.frp.bootstrap.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Yaml 解析工具类
 */
public class YamlParseUtil {

    private static final Yaml yaml = new Yaml();

    public static String getComponentScanPackage(String path) {
        try (InputStream inputStream = YamlParseUtil.class.getClassLoader().getResourceAsStream(path);) {
            Map<String, Object> config = yaml.load(inputStream);
        } catch (Exception e) {

        }
        return "";
    }
}
