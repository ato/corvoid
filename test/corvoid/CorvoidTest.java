package corvoid;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CorvoidTest {
    @Test
    public void capify() throws Exception {
        assertEquals("HelloThereWorld", Corvoid.capify("hello_there-world"));
    }

    @Test
    public void testOutdatedSortingAndInterpolation() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-outdated-test");
        try {
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom,
                    "<project>\n" +
                    "    <properties>\n" +
                    "        <junit.version>4.13.1</junit.version>\n" +
                    "    </properties>\n" +
                    "    <dependencies>\n" +
                    "        <dependency>\n" +
                    "            <groupId>org.yaml</groupId>\n" +
                    "            <artifactId>snakeyaml</artifactId>\n" +
                    "            <version>1.27</version>\n" +
                    "        </dependency>\n" +
                    "        <dependency>\n" +
                    "            <groupId>junit</groupId>\n" +
                    "            <artifactId>junit</artifactId>\n" +
                    "            <version>${junit.version}</version>\n" +
                    "        </dependency>\n" +
                    "    </dependencies>\n" +
                    "</project>");

            Corvoid corvoid = new Corvoid(tempDir);
            PrintStream oldOut = System.out;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));
            try {
                corvoid.outdated();
            } finally {
                System.setOut(oldOut);
            }

            String output = out.toString();
            List<String> lines = Arrays.asList(output.split("\\R"));
            lines.removeIf(String::isBlank);

            // Check if both are present and sorted
            // junit:junit should come before org.yaml:snakeyaml
            assertTrue("Output should contain junit:junit update", output.contains("junit:junit 4.13.1 ->"));
            assertTrue("Output should contain snakeyaml update", output.contains("org.yaml:snakeyaml 1.27 ->"));
            
            if (lines.size() >= 2) {
                assertTrue("Output should be sorted: " + output, lines.get(0).startsWith("junit:junit"));
                assertTrue("Output should be sorted: " + output, lines.get(1).startsWith("org.yaml:snakeyaml"));
            }
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testOutdatedDependencyManagement() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-dm-test");
        try {
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom,
                    "<project>\n" +
                    "    <dependencyManagement>\n" +
                    "        <dependencies>\n" +
                    "            <dependency>\n" +
                    "                <groupId>junit</groupId>\n" +
                    "                <artifactId>junit</artifactId>\n" +
                    "                <version>4.13.1</version>\n" +
                    "            </dependency>\n" +
                    "        </dependencies>\n" +
                    "    </dependencyManagement>\n" +
                    "    <dependencies>\n" +
                    "        <dependency>\n" +
                    "            <groupId>junit</groupId>\n" +
                    "            <artifactId>junit</artifactId>\n" +
                    "        </dependency>\n" +
                    "    </dependencies>\n" +
                    "</project>");

            Corvoid corvoid = new Corvoid(tempDir);
            PrintStream oldOut = System.out;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));
            try {
                corvoid.outdated();
            } finally {
                System.setOut(oldOut);
            }

            String output = out.toString();
            assertTrue("Output should contain junit:junit update from DM", output.contains("junit:junit 4.13.1 ->"));
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testOutdatedIgnoreUnstable() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-unstable-test");
        Path repoRoot = Files.createTempDirectory("corvoid-repo-root");
        try {
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom,
                    "<project>\n" +
                    "    <dependencies>\n" +
                    "        <dependency>\n" +
                    "            <groupId>org.example</groupId>\n" +
                    "            <artifactId>example-art</artifactId>\n" +
                    "            <version>1.0.0</version>\n" +
                    "        </dependency>\n" +
                    "    </dependencies>\n" +
                    "</project>");

            Path metadataFile = repoRoot.resolve("org/example/example-art/maven-metadata-central.xml");
            Files.createDirectories(metadataFile.getParent());
            
            Files.writeString(metadataFile,
                    "<metadata>\n" +
                    "  <groupId>org.example</groupId>\n" +
                    "  <artifactId>example-art</artifactId>\n" +
                    "  <versioning>\n" +
                    "    <release>1.1.0-beta1</release>\n" +
                    "    <versions>\n" +
                    "      <version>1.0.0</version>\n" +
                    "      <version>1.0.1</version>\n" +
                    "      <version>1.1.0-alpha1</version>\n" +
                    "      <version>1.1.0-beta1</version>\n" +
                    "    </versions>\n" +
                    "    <lastUpdated>20230101000000</lastUpdated>\n" +
                    "  </versioning>\n" +
                    "</metadata>");
            
            // Set last modified to now so it's not re-fetched (though it would fail to fetch from real maven central anyway)
            Files.setLastModifiedTime(metadataFile, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));

            Corvoid corvoid = new Corvoid(tempDir, repoRoot);
            PrintStream oldOut = System.out;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));
            try {
                corvoid.outdated();
            } finally {
                System.setOut(oldOut);
            }

            String output = out.toString();
            // Currently, it should pick 1.1.0-beta1 because it uses <release>
            // We want it to pick 1.0.1 and ignore alpha/beta.
            assertTrue("Output should contain update to 1.0.1, but was: " + output, output.contains("org.example:example-art 1.0.0 -> 1.0.1"));
            assertFalse("Output should NOT contain update to 1.1.0-beta1", output.contains("1.1.0-beta1"));
        } finally {
            deleteDirectory(tempDir);
            deleteDirectory(repoRoot);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}