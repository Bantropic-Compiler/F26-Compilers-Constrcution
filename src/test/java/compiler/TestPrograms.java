package compiler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestPrograms {
    private TestPrograms() {}

    public static String read(String name) {
        try {
            return Files.readString(Path.of("prog-examples", "test-cases", name));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read test program: " + name, e);
        }
    }
}
