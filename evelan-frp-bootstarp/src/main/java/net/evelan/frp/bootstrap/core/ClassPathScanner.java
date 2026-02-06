package net.evelan.frp.bootstrap.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 类路径扫描工具
 * 基于指定包名扫描所有类
 */
public class ClassPathScanner {

    public static Set<Class<?>> getPackageAllClasses(String packageName, boolean recursive) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new LinkedHashSet<>();
        String packageDirName = packageName.replace('.', '/');

        // 获取当前 thread 的类加载器
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // 通过 classloader 获取包对应的资源路径
        Enumeration<URL> dirs = classLoader.getResources(packageDirName);

        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            String protocol = url.getProtocol();

            if ("file".equals(protocol)) {
                // 是文件
                String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
                doScanPackageClassesByFile(classes, packageName, filePath, recursive);
            } else if ("jar".equals(protocol)) {
                // jar 包也扫描

            }
        }
        return classes;
    }

    /**
     * 递归扫描包下的所有类
     * @param classes 存放被扫描的类
     * @param packageName 包名
     * @param packagePath 包路径
     * @param recursive 是否递归
     * @throws ClassNotFoundException 找不到类
     */
    private static void doScanPackageClassesByFile(Set<Class<?>> classes, String packageName, String packagePath, boolean recursive) throws ClassNotFoundException{
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) return;

        // 获取包下的所有文件
        File[] files = dir.listFiles(file -> {
            if (file.isDirectory()) return recursive;
            return file.getName().endsWith(".class");
        });

        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                doScanPackageClassesByFile(
                        classes,
                        packageName + "." + file.getName(),
                        file.getAbsolutePath(),
                        recursive
                );
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                Class<?> clazz = loadClass(packageName + "." + className);
                if (clazz != null)
                    classes.add(clazz);
            }
        }
    }

    /**
     * 加载类 - 忽略内部类
     * @param fullClassName 全类名
     * @return 类对象
     */
    private static Class<?> loadClass(String fullClassName) {
        try {
            // 忽略内部类
            if (fullClassName.contains("$")) return null;
            return Thread.currentThread().getContextClassLoader().loadClass(fullClassName);
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + fullClassName);
            return null;
        }
    }
}
