package com.nutriplanner.testkit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Finds every *Test class under the given directory, runs its @Test methods and reports the results. */
public final class TestRunner {

    public static void main(String[] args) throws IOException {
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.setLevel(java.util.logging.Level.WARNING);
        for (java.util.logging.Handler h : rootLogger.getHandlers()) h.setLevel(java.util.logging.Level.WARNING);
        Path root = Paths.get(args.length > 0 ? args[0] : "build/test-classes");
        String only = System.getProperty("test.only", "");
        List<String> classes;
        try (Stream<Path> walk = Files.walk(root)) {
            classes = walk.map(p -> root.relativize(p).toString())
                    .filter(s -> s.endsWith("Test.class") && !s.contains("$"))
                    .map(s -> s.substring(0, s.length() - 6).replace('/', '.').replace('\\', '.'))
                    .filter(s -> only.isEmpty() || s.endsWith(only))
                    .sorted()
                    .collect(Collectors.toList());
        }
        int passed = 0;
        List<String> failures = new ArrayList<>();
        long started = System.currentTimeMillis();
        for (String className : classes) {
            Class<?> type;
            try {
                type = Class.forName(className);
            } catch (ClassNotFoundException e) {
                failures.add(className + ": cannot load class");
                continue;
            }
            List<Method> tests = new ArrayList<>();
            for (Method m : type.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Test.class)) tests.add(m);
            }
            if (tests.isEmpty()) continue;
            tests.sort(Comparator.comparing(Method::getName));
            System.out.println("  " + type.getSimpleName());
            for (Method m : tests) {
                long t0 = System.nanoTime();
                Object instance = null;
                try {
                    instance = type.getDeclaredConstructor().newInstance();
                    m.setAccessible(true);
                    m.invoke(instance);
                    passed++;
                    System.out.printf("    ok    %s (%d ms)%n", m.getName(), (System.nanoTime() - t0) / 1_000_000);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    failures.add(type.getSimpleName() + "." + m.getName() + ": " + cause);
                    System.out.printf("    FAIL  %s%n      %s%n", m.getName(), cause);
                    StackTraceElement[] trace = cause.getStackTrace();
                    for (int i = 0; i < Math.min(6, trace.length); i++) System.out.println("        at " + trace[i]);
                } catch (ReflectiveOperationException e) {
                    failures.add(type.getSimpleName() + "." + m.getName() + ": " + e);
                    System.out.printf("    FAIL  %s%n      %s%n", m.getName(), e);
                } finally {
                    if (instance instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) instance).close();
                        } catch (Exception ignored) {
                            // best effort cleanup
                        }
                    }
                }
            }
        }
        long seconds = (System.currentTimeMillis() - started) / 1000;
        System.out.println();
        System.out.printf("Tests: %d passed, %d failed (%d s)%n", passed, failures.size(), seconds);
        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            failures.forEach(f -> System.out.println("  - " + f));
            System.exit(1);
        }
    }
}
