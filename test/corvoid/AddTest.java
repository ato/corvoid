package corvoid;

import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;

public class AddTest {
    @Test
    public void testAddDuplicateDependency() throws Exception {
        File tempDir = Files.createTempDirectory("corvoid-test").toFile();
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            File pom = new File(tempDir, "pom.xml");
            Files.writeString(pom.toPath(), 
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
            
            String content = Files.readString(pom.toPath());
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
        File tempDir = Files.createTempDirectory("corvoid-test").toFile();
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            File pom = new File(tempDir, "pom.xml");
            Files.writeString(pom.toPath(), 
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
            
            String content = Files.readString(pom.toPath());
            assertTrue("Should contain version 2.0", content.contains("<version>2.0</version>"));
            assertTrue("Should preserve scope", content.contains("<scope>test</scope>"));
            assertTrue("Should preserve classifier", content.contains("<classifier>sources</classifier>"));
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testUpdateDependencyWithoutVersionTag() throws Exception {
        File tempDir = Files.createTempDirectory("corvoid-test").toFile();
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            File pom = new File(tempDir, "pom.xml");
            Files.writeString(pom.toPath(), 
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
            
            String content = Files.readString(pom.toPath());
            assertTrue("Should contain version 2.0", content.contains("<version>2.0</version>"));
            // Fallback logic currently loses scope if no version tag was present
            // but let's see what happens.
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testAddNewDependency() throws Exception {
        File tempDir = Files.createTempDirectory("corvoid-test").toFile();
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            File pom = new File(tempDir, "pom.xml");
            Files.writeString(pom.toPath(), 
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
            
            String content = Files.readString(pom.toPath());
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

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}
