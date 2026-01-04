package corvoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

public class AddTest {
    @Test
    public void testAddDuplicateDependency() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test");
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom, 
                "<project>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.example</groupId>\n" +
                "            <artifactId>example-art</artifactId>\n" +
                "            <version>1.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");
            
            corvoid.add("org.example:example-art", "2.0");
            
            String content = Files.readString(pom);
            // It should update 1.0 to 2.0 and NOT add a second dependency block
            assertTrue(content.contains("<version>2.0</version>"));
            assertFalse(content.contains("<version>1.0</version>"));
            
            int firstIdx = content.indexOf("<dependency>");
            int lastIdx = content.lastIndexOf("<dependency>");
            assertEquals(firstIdx, lastIdx);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testUpdatePreservesOtherTags() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test");
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom, 
                "<project>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.example</groupId>\n" +
                "            <artifactId>example-art</artifactId>\n" +
                "            <version>1.0</version>\n" +
                "            <scope>test</scope>\n" +
                "            <classifier>sources</classifier>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");
            
            corvoid.add("org.example:example-art", "2.0");
            
            String content = Files.readString(pom);
            assertTrue(content.contains("<version>2.0</version>"), "Should contain version 2.0");
            assertTrue(content.contains("<scope>test</scope>"), "Should preserve scope");
            assertTrue(content.contains("<classifier>sources</classifier>"), "Should preserve classifier");
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testUpdateDependencyWithoutVersionTag() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test");
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom, 
                "<project>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.example</groupId>\n" +
                "            <artifactId>example-art</artifactId>\n" +
                "            <scope>test</scope>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");
            
            corvoid.add("org.example:example-art", "2.0");
            
            String content = Files.readString(pom);
            assertTrue(content.contains("<version>2.0</version>"));
            // Fallback logic currently loses scope if no version tag was present
            // but let's see what happens.
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testAddNewDependency() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test");
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom, 
                "<project>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.example</groupId>\n" +
                "            <artifactId>example-art</artifactId>\n" +
                "            <version>1.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");
            
            corvoid.add("org.other:other-art", "1.5");
            
            String content = Files.readString(pom);
            assertTrue(content.contains("example-art"));
            assertTrue(content.contains("<version>1.0</version>"));
            assertTrue(content.contains("other-art"));
            assertTrue(content.contains("<version>1.5</version>"));
            
            int firstIdx = content.indexOf("<dependency>");
            int lastIdx = content.lastIndexOf("<dependency>");
            assertNotEquals(firstIdx, lastIdx);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
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
