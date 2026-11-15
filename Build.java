import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Nutri Planner build tool, written in Java so the project needs nothing but a JDK.
 *
 * <pre>
 *   java Build.java            compile, run all tests, and package dist/nutri-planner.jar
 *   java Build.java compile    compile main sources only
 *   java Build.java test       compile and run the test suite
 *   java Build.java package    compile and package without running tests
 *   java Build.java run        package (skipping tests) and start the web server
 *   java Build.java clean      delete build output
 * </pre>
 */
public class Build {

    static final Path ROOT = Paths.get("").toAbsolutePath();
    static final Path SRC = ROOT.resolve("src/main/java");
    static final Path RESOURCES = ROOT.resolve("src/main/resources");
    static final Path TEST_SRC = ROOT.resolve("src/test/java");
    static final Path LIB = ROOT.resolve("lib");
    static final Path OUT = ROOT.resolve("build");
    static final Path CLASSES = OUT.resolve("classes");
    static final Path TEST_CLASSES = OUT.resolve("test-classes");
    static final Path DIST = ROOT.resolve("dist");
    static final String MAIN_CLASS = "com.nutriplanner.NutriPlannerApplication";
    static final String TEST_RUNNER = "com.nutriplanner.testkit.TestRunner";
    static final String JAR_NAME = "nutri-planner.jar";
    static final String JAVA_RELEASE = "17";

    public static void main(String[] args) throws Exception {
        if (!Files.isDirectory(SRC)) {
            fail("Run this from the project folder (the one containing Build.java and src/).");
        }
        List<String> commands = args.length == 0 ? List.of("compile", "test", "package") : Arrays.asList(args);
        for (String command : commands) {
            switch (command.toLowerCase(Locale.ROOT)) {
                case "clean" -> clean();
                case "compile" -> compileMain();
                case "test" -> { compileMain(); compileTests(); runTests(); }
                case "package" -> { compileMain(); packageJar(); }
                case "run" -> { compileMain(); packageJar(); run(); }
                default -> fail("Unknown command: " + command + ". Use compile, test, package, run or clean.");
            }
        }
    }

    static void clean() throws IOException {
        deleteTree(OUT);
        deleteTree(DIST);
        log("Cleaned build output.");
    }

    static boolean mainCompiled = false;

    static void compileMain() throws IOException {
        if (mainCompiled) return;
        deleteTree(CLASSES);
        Files.createDirectories(CLASSES);
        compile(javaFiles(SRC), CLASSES, libraryClasspath());
        copyTree(RESOURCES, CLASSES);
        mainCompiled = true;
        log("Compiled main sources into " + ROOT.relativize(CLASSES));
    }

    static void compileTests() throws IOException {
        deleteTree(TEST_CLASSES);
        Files.createDirectories(TEST_CLASSES);
        List<String> cp = new ArrayList<>(libraryClasspath());
        cp.add(CLASSES.toString());
        compile(javaFiles(TEST_SRC), TEST_CLASSES, cp);
        log("Compiled tests into " + ROOT.relativize(TEST_CLASSES));
    }

    static void runTests() throws IOException, InterruptedException {
        List<String> cp = new ArrayList<>(libraryClasspath());
        cp.add(CLASSES.toString());
        cp.add(TEST_CLASSES.toString());
        List<String> cmd = new ArrayList<>(List.of(javaExecutable(), "-Djava.awt.headless=true", "-cp",
                String.join(File.pathSeparator, cp), TEST_RUNNER, TEST_CLASSES.toString()));
        int exit = new ProcessBuilder(cmd).inheritIO().start().waitFor();
        if (exit != 0) fail("Tests failed.");
    }

    static void packageJar() throws IOException {
        Files.createDirectories(DIST);
        Path jar = DIST.resolve(JAR_NAME);
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, MAIN_CLASS);
        attributes.put(new Attributes.Name("Multi-Release"), "true");
        attributes.put(new Attributes.Name("Implementation-Title"), "Nutri Planner");
        attributes.put(new Attributes.Name("Implementation-Version"), "1.0.0");
        Set<String> written = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            written.add("META-INF/MANIFEST.MF");
            List<Path> files;
            try (Stream<Path> walk = Files.walk(CLASSES)) {
                files = walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
            }
            for (Path file : files) {
                String name = CLASSES.relativize(file).toString().replace(File.separatorChar, '/');
                addDirectories(out, name, written);
                out.putNextEntry(new JarEntry(name));
                Files.copy(file, out);
                out.closeEntry();
                written.add(name);
            }
            for (String lib : libraryClasspath()) {
                try (JarFile dependency = new JarFile(lib)) {
                    var entries = dependency.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (entry.isDirectory() || written.contains(name) || isSignatureOrManifest(name)) continue;
                        addDirectories(out, name, written);
                        out.putNextEntry(new JarEntry(name));
                        try (InputStream in = dependency.getInputStream(entry)) {
                            in.transferTo(out);
                        }
                        out.closeEntry();
                        written.add(name);
                    }
                }
            }
        }
        log("Packaged " + ROOT.relativize(jar) + " (" + (Files.size(jar) / 1024) + " KB)");
    }

    static void run() throws IOException, InterruptedException {
        Path jar = DIST.resolve(JAR_NAME);
        log("Starting Nutri Planner. Press Ctrl+C to stop.");
        int exit = new ProcessBuilder(javaExecutable(), "-jar", jar.toString()).inheritIO().start().waitFor();
        System.exit(exit);
    }

    // ---------------------------------------------------------------- helpers

    static void compile(List<Path> sources, Path output, List<String> classpath) throws IOException {
        if (sources.isEmpty()) return;
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) fail("No Java compiler found. Install a JDK (17 or newer), not just a JRE.");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<String> options = new ArrayList<>(List.of(
                    "--release", JAVA_RELEASE, "-encoding", "UTF-8", "-parameters",
                    "-Xlint:unchecked,deprecation,rawtypes", "-d", output.toString()));
            if (!classpath.isEmpty()) {
                options.add("-cp");
                options.add(String.join(File.pathSeparator, classpath));
            }
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(sources);
            boolean ok = compiler.getTask(null, fm, diagnostics, options, null, units).call();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                String where = d.getSource() == null ? "" : ROOT.relativize(Paths.get(d.getSource().toUri())) + ":" + d.getLineNumber() + ": ";
                System.err.println(d.getKind().toString().toLowerCase(Locale.ROOT) + ": " + where + d.getMessage(Locale.ROOT));
            }
            if (!ok) fail("Compilation failed.");
        }
    }

    static List<Path> javaFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }
    }

    static List<String> libraryClasspath() throws IOException {
        if (!Files.isDirectory(LIB)) return List.of();
        try (Stream<Path> list = Files.list(LIB)) {
            return list.filter(p -> p.toString().endsWith(".jar")).sorted().map(Path::toString).collect(Collectors.toList());
        }
    }

    static boolean isSignatureOrManifest(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.equals("META-INF/MANIFEST.MF")) return true;
        if (!upper.startsWith("META-INF/") || upper.indexOf('/', 9) >= 0) return false;
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC");
    }

    static void addDirectories(JarOutputStream out, String name, Set<String> written) throws IOException {
        int idx = name.indexOf('/');
        while (idx >= 0) {
            String dir = name.substring(0, idx + 1);
            if (written.add(dir)) {
                out.putNextEntry(new JarEntry(dir));
                out.closeEntry();
            }
            idx = name.indexOf('/', idx + 1);
        }
    }

    static void copyTree(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) return;
        Files.walkFileTree(from, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(to.resolve(from.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, to.resolve(from.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).collect(Collectors.toList())) {
                Files.deleteIfExists(p);
            }
        }
    }

    static String javaExecutable() {
        String exe = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return Paths.get(System.getProperty("java.home"), "bin", exe).toString();
    }

    static void log(String message) {
        System.out.println("[build] " + message);
    }

    static void fail(String message) {
        System.err.println("[build] " + message);
        System.exit(1);
    }
}
