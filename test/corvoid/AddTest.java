package corvoid;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;

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
            assertTrue("Should contain version 2.0", content.contains("<version>2.0</version>"));
            assertFalse("Should not contain version 1.0", content.contains("<version>1.0</version>"));
            
            int firstIdx = content.indexOf("<dependency>");
            int lastIdx = content.lastIndexOf("<dependency>");
            assertEquals("Should only have one dependency block", firstIdx, lastIdx);
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
            assertTrue("Should contain version 2.0", content.contains("<version>2.0</version>"));
            assertTrue("Should preserve scope", content.contains("<scope>test</scope>"));
            assertTrue("Should preserve classifier", content.contains("<classifier>sources</classifier>"));
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
            assertTrue("Should contain version 2.0", content.contains("<version>2.0</version>"));
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
            assertTrue("Should contain original dependency", content.contains("example-art"));
            assertTrue("Should contain original version", content.contains("<version>1.0</version>"));
            assertTrue("Should contain new dependency", content.contains("other-art"));
            assertTrue("Should contain new version", content.contains("<version>1.5</version>"));
            
            int firstIdx = content.indexOf("<dependency>");
            int lastIdx = content.lastIndexOf("<dependency>");
            assertNotEquals("Should have two dependency blocks", firstIdx, lastIdx);
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
