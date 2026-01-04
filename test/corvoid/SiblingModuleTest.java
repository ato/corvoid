package corvoid;

import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SiblingModuleTest {

    @Test
    public void testSiblingResolution() throws IOException, XMLStreamException {
        Path tempDir = Files.createTempDirectory("corvoid-sibling-test");
        try {
            Path parentPom = tempDir.resolve("pom.xml");
            Files.writeString(parentPom, 
                "<project>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <groupId>test</groupId>" +
                "  <artifactId>parent</artifactId>" +
                "  <version>1.0</version>" +
                "  <packaging>pom</packaging>" +
                "  <modules>" +
                "    <module>child1</module>" +
                "    <module>child2</module>" +
                "  </modules>" +
                "</project>");

            Path child1Dir = tempDir.resolve("child1");
            Files.createDirectories(child1Dir);
            Path child1Pom = child1Dir.resolve("pom.xml");
            Files.writeString(child1Pom, 
                "<project>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <parent>" +
                "    <groupId>test</groupId>" +
                "    <artifactId>parent</artifactId>" +
                "    <version>1.0</version>" +
                "  </parent>" +
                "  <artifactId>child1</artifactId>" +
                "</project>");

            Path child2Dir = tempDir.resolve("child2");
            Files.createDirectories(child2Dir);
            Path child2Pom = child2Dir.resolve("pom.xml");
            Files.writeString(child2Pom, 
                "<project>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <parent>" +
                "    <groupId>test</groupId>" +
                "    <artifactId>parent</artifactId>" +
                "    <version>1.0</version>" +
                "  </parent>" +
                "  <artifactId>child2</artifactId>" +
                "  <dependencies>" +
                "    <dependency>" +
                "      <groupId>test</groupId>" +
                "      <artifactId>child1</artifactId>" +
                "      <version>1.0</version>" +
                "    </dependency>" +
                "  </dependencies>" +
                "</project>");

            Corvoid corvoid = new Corvoid(child2Dir);
            DependencyTree tree = corvoid.tree();
            
            // Verify child1 is resolved as a local module
            DependencyTree.Node root = tree.root();
            assertEquals("child2", root.getModel().getArtifactId());
            assertEquals(1, root.children().size());
            
            DependencyTree.Node child1Node = root.children().get(0);
            assertEquals("child1", child1Node.getModel().getArtifactId());
            
            Path child1Path = child1Node.artifactPath();
            assertTrue(child1Path.endsWith(Path.of("child1/target/classes")), "Path should point to target/classes: " + child1Path);
            
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }
}
