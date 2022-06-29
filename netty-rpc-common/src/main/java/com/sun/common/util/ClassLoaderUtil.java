package com.sun.common.util;


public final class ClassLoaderUtil {

    // 获取类加载器
    // 一般来说，上下文类加载器要比当前类加载器更适合于框架编程，而当前类加载器则更适合于业务逻辑编程。
    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader classLoader = null;
        try {
            //获取当前线程的context class loader
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (RuntimeException e) {
        }
        
        if (null == classLoader) {
            // 如果没有context loader，使用当前类的类加载器
            classLoader = clazz.getClassLoader();
            if (null == classLoader) {
                // 如果当前类加载器无法获取，则尝试获取bootstrap ClassLoader
                try {
                    classLoader = ClassLoader.getSystemClassLoader();
                } catch (RuntimeException e) {
                }
            }
        }
        return classLoader;
    }
    
    
    private ClassLoaderUtil() {
        throw new IllegalStateException("Instantiation not allowed");
    }
}