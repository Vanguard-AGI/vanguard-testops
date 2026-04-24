package io.vanguard.testops.web.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebControllerArchitectureTest {

    @Test
    void controllerShouldNotImportMapper() throws IOException {
        Path sourceRoot = Paths.get("src/main/java");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains("/controller/"))
                    .forEach(path -> collectViolation(path, violations));
        }
        assertTrue(violations.isEmpty(), "Controller must not import mapper directly.\n" + String.join("\n", violations));
    }

    private void collectViolation(Path file, List<String> violations) {
        try (Stream<String> lines = Files.lines(file)) {
            lines.filter(line -> line.startsWith("import "))
                    .filter(line -> line.contains(".mapper."))
                    .forEach(line -> violations.add(file + " -> " + line.trim()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file, e);
        }
    }
}
