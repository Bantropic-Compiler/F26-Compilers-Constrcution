package compiler.codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Assembles all classes from one compilation before publishing its outputs. */
public final class Jasmin {
    private Jasmin() {}

    public static void assemble(Map<String, String> sources, Path output) throws IOException {
        Path jar = Path.of("lib", "jasmin.jar").toAbsolutePath();
        if (!Files.isRegularFile(jar)) {
            throw new IOException("Missing Jasmin assembler: " + jar);
        }
        Files.createDirectories(output);
        Path stage = Files.createTempDirectory(output, ".jasmin-");
        try {
            String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            var command = new ArrayList<>(List.of(executable, "-jar", jar.toString(), "-d", "."));
            List<String> products = new ArrayList<>();
            for (var source : sources.entrySet()) {
                String name = source.getKey();
                Files.writeString(stage.resolve(name), source.getValue());
                command.add(name);
                products.add(name);
                products.add(name.substring(0, name.length() - 2) + ".class");
            }
            Process process = new ProcessBuilder(command).directory(stage.toFile())
                    .redirectErrorStream(true).start();
            String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            try {
                // Jasmin 2.4 can report syntax errors with a successful exit status.
                if (process.waitFor() != 0 || products.stream().anyMatch(name -> !Files.isRegularFile(stage.resolve(name)))) {
                    throw new IOException("Jasmin assembly failed:\n" + log.strip());
                }
            } catch (InterruptedException error) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("Jasmin assembly interrupted", error);
            }
            for (String name : products) {
                Files.move(stage.resolve(name), output.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try (var files = Files.newDirectoryStream(stage)) {
                for (Path file : files) Files.delete(file);
            }
            Files.delete(stage);
        }
    }
}
