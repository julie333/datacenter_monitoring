package com.example.datacenter_monitoring.util;

public class Assert {
    private Assert() {}
    public static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
