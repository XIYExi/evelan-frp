package net.evelan.frp.bootstrap.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Yaml 解析工具类
 */
@SuppressWarnings("unused")
public class YamlParseUtil {

    private static final Yaml yaml = new Yaml();

    public static Map<String, Object> loadYamlPlainMap(String path) {
        Map<String, Object> data = loadYaml(path);
        Map<String, Object> plain = new LinkedHashMap<>();
        convertTo(data, "", plain);
        return plain;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYaml(String path) {
        var loaderOptions = new LoaderOptions();
        var dumperOptions = new DumperOptions();
        var representer = new Representer(dumperOptions);
        var resolver = new Resolver();
        var yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
        return ClassPathUtils.readInputStream(path, (input) -> {
            return (Map<String, Object>) yaml.load(input);
        });
    }


    /**
     * 递归将树状结构变成扁平结构
     * Yaml解析出来的格式是嵌套Map，保留yaml中的格式，如 server:port:8080,现在需要嵌套修改为 server.port:8080, 这样才符合需要的值，方便后续Value和Properties阅读和解析
     * @param source yaml utils解析出来的原字典，也是每一层递归的输入
     * @param prefix 前缀，默认是空，后续为当前节点的父节点
     * @param plain 输出字典
     */
    static void convertTo(Map<String, Object> source, String prefix, Map<String, Object> plain) {
        for (String key : source.keySet()) {
            Object value = source.get(key);
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) value;
                convertTo(subMap, prefix + key + ".", plain);
            } else if (value instanceof List) {
                plain.put(prefix + key, value);
            } else {
                plain.put(prefix + key, value.toString());
            }
        }
    }
}
