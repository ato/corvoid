package corvoid;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateTest {

    @Test
    public void testUpdateAll() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test-update");
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom, 
                "<project>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.apache.xmlbeans</groupId>\n" +
                "            <artifactId>xmlbeans</artifactId>\n" +
                "            <version>3.1.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");

            corvoid.command(new String[]{"update"});
            
            String content = Files.readString(pom);
            // We don't know the exact latest version, but it should be different from 3.1.0 if an update was found.
            // xmlbeans 3.1.0 is quite old.
            assertFalse(content.contains("<version>3.1.0</version>"), "Should not contain old version 3.1.0");
            assertTrue(content.contains("<version>"), "Should contain some version tag");
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
    @Test
    public void testUpdateSpecific() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test-update-specific");
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            
            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom, 
                "<project>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.apache.xmlbeans</groupId>\n" +
                "            <artifactId>xmlbeans</artifactId>\n" +
                "            <version>3.1.0</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>org.jdbi</groupId>\n" +
                "            <artifactId>jdbi3-sqlite</artifactId>\n" +
                "            <version>3.10.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");

            // Only update xmlbeans
            corvoid.command(new String[]{"update", "org.apache.xmlbeans:xmlbeans"});
            
            String content = Files.readString(pom);
            assertFalse(content.contains("<version>3.1.0</version>"), "xmlbeans should be updated");
            assertTrue(content.contains("<version>3.10.0</version>"), "jdbi should NOT be updated");
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testUpdateInDependencyManagement() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test-update-dm");
        try {
            Corvoid corvoid = new Corvoid(tempDir);

            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom,
                "<project>\n" +
                "    <dependencyManagement>\n" +
                "        <dependencies>\n" +
                "            <dependency>\n" +
                "                <groupId>org.apache.xmlbeans</groupId>\n" +
                "                <artifactId>xmlbeans</artifactId>\n" +
                "                <version>3.1.0</version>\n" +
                "            </dependency>\n" +
                "        </dependencies>\n" +
                "    </dependencyManagement>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.apache.xmlbeans</groupId>\n" +
                "            <artifactId>xmlbeans</artifactId>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");

            corvoid.command(new String[]{"update"});

            String content = Files.readString(pom);
            assertFalse(content.contains("<version>3.1.0</version>"), "dependencyManagement version should be updated");

            // Check if it added a version to the dependencies section
            int depsIndex = content.indexOf("<dependencies>");
            int dmDepsIndex = content.indexOf("<dependencies>", content.indexOf("<dependencyManagement>"));
            int projectDepsIndex = content.lastIndexOf("<dependencies>");

            // projectDepsIndex should be the one in <dependencies> (not management)
            String dependenciesSection = content.substring(projectDepsIndex);
            assertFalse(dependenciesSection.contains("<version>"), "Should NOT have added version to dependencies section");
        } finally {
            deleteDirectory(tempDir);
        }
    }
    @Test
    public void testUpdateMultipleDependencies() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test-update-multiple");
        try {
            Corvoid corvoid = new Corvoid(tempDir);

            Path pom = tempDir.resolve("pom.xml");
            Files.writeString(pom,
                "<project>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.apache.xmlbeans</groupId>\n" +
                "            <artifactId>xmlbeans</artifactId>\n" +
                "            <version>3.1.0</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>org.jdbi</groupId>\n" +
                "            <artifactId>jdbi3-sqlite</artifactId>\n" +
                "            <version>3.10.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");

            corvoid.command(new String[]{"update"});

            String content = Files.readString(pom);
            assertFalse(content.contains("<version>3.1.0</version>"), "xmlbeans should be updated");
            assertFalse(content.contains("<version>3.10.0</version>"), "jdbi should be updated");
            assertTrue(content.contains("xmlbeans"), "Should still have xmlbeans");
            assertTrue(content.contains("jdbi3-sqlite"), "Should still have jdbi3-sqlite");
            
            // Check for potential corruption
            assertTrue(content.contains("</project>"), "Should have proper project closing tag");
            int count = 0;
            int lastIndex = 0;
            while ((lastIndex = content.indexOf("<dependency>", lastIndex)) != -1) {
                count++;
                lastIndex += "<dependency>".length();
            }
            assertEquals(2, count, "Should have exactly 2 dependencies");
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    public void testUpdateWithBom() throws Exception {
        Path tempDir = Files.createTempDirectory("corvoid-test-bom-update");
        try {
            Corvoid corvoid = new Corvoid(tempDir);
            Path pom = tempDir.resolve("pom.xml");
            
            // We use Jackson BOM as an example.
            // version 2.12.0 is old.
            Files.writeString(pom,
                "<project>\n" +
                "    <dependencyManagement>\n" +
                "        <dependencies>\n" +
                "            <dependency>\n" +
                "                <groupId>com.fasterxml.jackson</groupId>\n" +
                "                <artifactId>jackson-bom</artifactId>\n" +
                "                <version>2.12.0</version>\n" +
                "                <type>pom</type>\n" +
                "                <scope>import</scope>\n" +
                "            </dependency>\n" +
                "        </dependencies>\n" +
                "    </dependencyManagement>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>com.fasterxml.jackson.core</groupId>\n" +
                "            <artifactId>jackson-databind</artifactId>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>");

            try {
                corvoid.command(new String[]{"update"});
            } catch (Exception e) {
                if (Files.exists(pom)) {
                    System.out.println("Corrupted POM content:\n" + Files.readString(pom));
                }
                throw e;
            }

            String content = Files.readString(pom);
            
            // The issue is that the second pass should see jackson-databind as already managed by the NEW BOM version.
            // If it doesn't re-parse, it might think it needs to add a version to jackson-databind.
            
            int projectDepsIndex = content.lastIndexOf("<dependencies>");
            String dependenciesSection = content.substring(projectDepsIndex);
            assertFalse(dependenciesSection.contains("<version>"), "Should NOT have added version to jackson-databind in dependencies section");
        } finally {
            deleteDirectory(tempDir);
        }
    }
}
