package io.vanguard.testops.architecture;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureGuardTest {

    private static final Set<String> COMMON_STARTER_ALLOWLIST = Set.of(
            "spring-boot-starter-aop",
            "spring-boot-starter-web",
            "spring-boot-starter-jetty",
            "spring-boot-starter-websocket",
            "spring-boot-starter-mail",
            "spring-boot-starter-data-ldap",
            "spring-boot-starter-validation"
    );

    @Test
    void serviceShouldNotImportWebPackage() throws IOException {
        Path serviceRoot = locateRepoRoot().resolve("vanguard-testops-service/src/main/java");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(serviceRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectImportViolation(path, "import io.vanguard.testops.web.", violations));
        }
        assertTrue(violations.isEmpty(), "Service layer must not import web package.\n" + String.join("\n", violations));
    }

    @Test
    void mapperShouldBeInDaoOrServiceMapperPackage() throws IOException {
        Path repoRoot = locateRepoRoot();
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(repoRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("Mapper.java"))
                    .filter(path -> path.toString().contains("/src/main/java/") || path.toString().contains("\\src\\main\\java\\"))
                    .forEach(path -> {
                        String normalized = repoRoot.relativize(path).toString().replace('\\', '/');
                        boolean inDao = normalized.startsWith("vanguard-testops-dao/src/main/java/");
                        boolean inServiceMapper = normalized.startsWith("vanguard-testops-service/src/main/java/")
                                && normalized.contains("/mapper/");
                        if (!inDao && !inServiceMapper) {
                            violations.add(normalized);
                        }
                    });
        }
        assertTrue(violations.isEmpty(),
                "Mapper must be in dao module, or service transitional mapper package.\n" + String.join("\n", violations));
    }

    @Test
    void commonModuleShouldNotAddNewSpringBootStarters() throws Exception {
        Path commonPom = locateRepoRoot().resolve("vanguard-testops-common/pom.xml");
        var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var document = builder.parse(commonPom.toFile());
        var expression = "/*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency']/*[local-name()='artifactId']/text()";
        var nodes = (org.w3c.dom.NodeList) XPathFactory.newInstance()
                .newXPath()
                .compile(expression)
                .evaluate(document, XPathConstants.NODESET);
        Set<String> starterIds = new java.util.HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            String artifactId = nodes.item(i).getNodeValue();
            if (artifactId != null && artifactId.startsWith("spring-boot-starter")) {
                starterIds.add(artifactId.trim());
            }
        }
        Set<String> unexpected = starterIds.stream()
                .filter(id -> !COMMON_STARTER_ALLOWLIST.contains(id))
                .collect(Collectors.toSet());
        assertTrue(unexpected.isEmpty(),
                "Common module introduced new spring-boot-starter dependencies: " + unexpected);
    }

    private void collectImportViolation(Path file, String forbiddenFragment, List<String> violations) {
        try (Stream<String> lines = Files.lines(file)) {
            lines.filter(line -> line.startsWith("import "))
                    .filter(line -> line.contains(forbiddenFragment))
                    .forEach(line -> violations.add(file + " -> " + line.trim()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read: " + file, e);
        }
    }

    private Path locateRepoRoot() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("vanguard-testops-dao")) && Files.exists(cwd.resolve("pom.xml"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("vanguard-testops-dao")) && Files.exists(parent.resolve("pom.xml"))) {
            return parent;
        }
        throw new IllegalStateException("Cannot locate repository root from " + cwd);
    }
}
