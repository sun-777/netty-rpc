package com.sun.common.util;

/**
 * @description:
 * @author: Sun Xiaodong
 */
public final class Assertions {

    public static <T> T notNull(final String name, final T value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " can not be null");
        }
        return value;
    }


    public static void isTrueArgument(final String name, final boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException("state should be: " + name);
        }
    }

    public static <T> T isTrueArgument(final String name, final T value, final boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException("state should be: " + name);
        }
        return value;
    }


    // validate range in: [min, max]
    public static void rangeIn(final int current, int min, int max) {
        if (Math.max(0, current) != Math.min(current, max)) {
            throw new IllegalArgumentException(String.format("The given number %d can't be greater than %d or less than %d", current, max, min));
        }
    }


    private Assertions() {
        throw new IllegalStateException("Instantiation not allowed");
    }
}
