package com.sun.common.enumerator;

import java.util.Arrays;
import java.util.Optional;

/**
 *  See: <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.1.2">https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.1.2</a>
 *  <p>AdditionalBound:
 *      & InterfaceType
 * 
 *  <p>The enum values are initialized before any other static fields.
 * 
 *  @author Sun xiaodong
 */
public interface CodeEnum<T extends Enum<T> & CodeEnum<T, C>, C> {
    C code();
    
    /**
     * 根据给定的枚举对象的属性值c，查找枚举对象
     * @param c  枚举对象的属性值
     * @return  返回枚举类的Optional对象：如果枚举类T中有c值，则返回c值对应的枚举类T的Optional对象； 没有则返回Optional的空的实例。
     */
    Optional<T> codeOf(C c);
    
    /**
     * 根据给定的枚举对象的属性值e，查找枚举对象
     * @param enumClass  枚举类型类T
     * @param c  枚举类的属性值
     * @return  返回枚举类的Optional对象：如果枚举类T中有c值，则返回c值对应的枚举类T的Optional对象； 没有则返回Optional的空的实例。
     * @param <T>  枚举类的泛型声明
     * @param <C>  枚举类属性的泛型声明
     */
    static <T extends Enum<T> & CodeEnum<T, C>, C> Optional<T> codeOf(Class<T> enumClass, C c)  {
        if (c instanceof String) {   //如果是String类型，则不分大小写进行比较
            return Arrays.stream(enumClass.getEnumConstants()).filter(o -> ((String) o.code()).equalsIgnoreCase((String) c)).findAny();
        } else {
            return Arrays.stream(enumClass.getEnumConstants()).filter(o -> o.code().equals(c)).findAny();
        }
    }
}
