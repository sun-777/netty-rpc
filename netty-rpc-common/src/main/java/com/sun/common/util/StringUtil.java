package com.sun.common.util;

import java.util.Optional;

/**
 * 字符串工具类
 */
public final class StringUtil {

    public static boolean hasText(String str) {
        return !isBlank(str);
    }

    /**
     * 检查字符串是否为空字符串（null也是空字符串）
     * @param str
     * @return  true: 是空字符串； false：非空字符串
     */
    public static boolean isBlank(String str) {
        if (null != str && !str.isEmpty()) {
            for (int i = 0, length = str.length(); i < length; i++) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }


    public static boolean isBlank(Optional<String> optional) {
        return optional.isPresent() ? isBlank(optional.get()) : true;
    }


    /**
     * 字符串数组中是否存在空字符串
     * @param strings  待检查的字符串数组
     * @return  true: 存在为null或为空的字符串； false：不存在空字符串
     */
    public static boolean existsBlank(String[] strings) {
        boolean isEmpty = false;
        for (int i = 0; i < strings.length && !isEmpty; i++) {
            isEmpty |= isBlank(strings[i]);
        }
        return isEmpty;
    }

    
    /**
     * 去除字符串首尾空字符（空字符为：空格、回车、换行）
     * @param str
     * @return
     */
    public static String strip(String str) {
        if (null == str) {
            return null;
        }
        final int length = str.length();
        int head = 0, tail = length - 1;
        for (int i = head, j = tail; head < tail;) {
            if (-1 != i && i < tail) {
                final char ch = str.charAt(i);
                if (Character.isWhitespace(ch)) {
                    i ++;
                } else {
                    head = i;
                    i = -1;
                }
            }
            if (-1 != j && j > head) {
                final char ch = str.charAt(j);
                if (Character.isWhitespace(ch)) {
                    --j;
                } else {
                    tail = j;
                    j = -1;
                }
            }
            if (-1 == i && -1 == j) {
                break;
            }
        }

        if (head > tail) {
            return "";
        } else {
            return str.substring(head, tail + 1);
        }
    }
    

    private StringUtil() {
        throw new IllegalStateException("Instantiation not allowed");
    }
}
