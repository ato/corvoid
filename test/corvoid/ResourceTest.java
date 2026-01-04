package corvoid;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceTest {

    @Test
    public void testCopyResources() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-resource-test");
        try {
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom,
                    "<project>\n" +
                    "    <groupId>test</groupId>\n" +
                    "    <artifactId>test-resources</artifactId>\n" +
                    "    <version>1.0</version>\n" +
                    "    <build>\n" +
                    "        <resources>\n" +
                    "            <resource>\n" +
                    "                <directory>res</directory>\n" +
                    "            </resource>\n" +
                    "            <resource>\n" +
                    "                <directory>res2</directory>\n" +
                    "                <targetPath>subdir</targetPath>\n" +
                    "            </resource>\n" +
                    "        </resources>\n" +
                    "    </build>\n" +
                    "</project>");

            Path resDir = tempDir.resolve("res");
            Files.createDirectories(resDir);
            Files.writeString(resDir.resolve("config.properties"), "key=value");

            Path resDir2 = tempDir.resolve("res2");
            Files.createDirectories(resDir2);
            Files.writeString(resDir2.resolve("extra.txt"), "some content");

            Corvoid corvoid = new Corvoid(tempDir);
            corvoid.command(new String[]{"compile"});

            assertTrue(Files.exists(tempDir.resolve("target/classes/config.properties")), "config.properties should be copied");
            assertTrue(Files.exists(tempDir.resolve("target/classes/subdir/extra.txt")), "extra.txt should be copied to subdir");
            
            // Test update
            Thread.sleep(1000); // Ensure timestamp change
            Files.writeString(resDir.resolve("config.properties"), "key=newvalue");
            corvoid.command(new String[]{"compile"});
            
            assertTrue(Files.readString(tempDir.resolve("target/classes/config.properties")).contains("newvalue"), "config.properties should be updated");

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
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
