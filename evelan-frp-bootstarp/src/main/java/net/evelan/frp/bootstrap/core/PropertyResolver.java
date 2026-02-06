package net.evelan.frp.bootstrap.core;

import net.evelan.frp.bootstrap.entity.PropertyExpr;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 属性解析器 用于保存所有的配置项，对外提供查询功能
 * 1. 支持按照配置的key查询，如 getProperty("name")
 * 2. 支持按照 ${abc.xyz} 方式查询，用于 @EValue("abc.xyz")
 * 3. 带默认值，以${abc.xyz:defaultValue}形式的查询，例如，getProperty("${app.title:1}")，常用于@Value("${app.title:1}")注入。
 */
public class PropertyResolver {
    // 存储所有的配置项(包括环境变量)
    Map<String, String> properties = new ConcurrentHashMap<>();
    // 存储 Class -> Function
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        // 环境变量先直接都存进去
        this.properties.putAll(System.getenv());
        // 再存传进来的properties
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, properties.get(name));
        }

        converters.put(String.class, value -> value);
        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::valueOf);
        converters.put(int.class, Integer::parseInt);
        converters.put(Integer.class, Integer::valueOf);
        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::valueOf);
        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::valueOf);
        converters.put(float.class, Float::parseFloat);
        converters.put(Float.class, Float::valueOf);
        converters.put(char.class, value -> value.charAt(0));
        converters.put(Character.class, value -> value.charAt(0));
        converters.put(byte.class, Byte::parseByte);
        converters.put(Byte.class, Byte::valueOf);
        converters.put(short.class, Short::parseShort);
        converters.put(Short.class, Short::valueOf);
        converters.put(Object.class, value -> value);

        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Duration.class, Duration::parse);
        converters.put(ZoneId.class, ZoneId::of);
    }


    public PropertyExpr parserPropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            int n = key.indexOf(":");
            if (n == -1) {
                // 不存在defaultValue
                // 如果key符合格式，也就是${开头，那么应该从第三个字符开始截取（索引2）,结尾同理，需要去除}符号
                String k = key.substring(2, key.length() - 1);
                return new PropertyExpr(k, null);
            } else {
                // 有默认值 ${key: default}
                String k = key.substring(2, n);
                return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
            }
        }
        return null;
    }

    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }


    public String getProperty(String key) {
        // 按 ${abc.xyz:defaultValue} 格式解析
        PropertyExpr expr = parserPropertyExpr(key);
        if (expr != null) {
            if (expr.defaultValue() != null)
                return getProperty(expr.key(), expr.defaultValue());
            else
                return getRequiredProperty(expr.key());
        }
        String value = this.properties.get(key);
        if (value != null)
            return parseValue(value);
        return value;
    }


    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null)
            return null;
        return convert(targetType, value);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }



    @SuppressWarnings("unchecked")
    <T> T convert(Class<T> clazz, String value) {
        Function<String, Object> fn = converters.get(clazz);
        if (fn == null)
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        return (T) fn.apply(value);
    }


    String parseValue(String value) {
        PropertyExpr expr = parserPropertyExpr(value);
        if (expr == null)
            return value;
        if (expr.defaultValue() != null)
            return getProperty(expr.key(), expr.defaultValue());
        else
            return getRequiredProperty(expr.key());
    }

    String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }

}
