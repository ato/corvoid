package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Parent {
    private String artifactId;
    private String groupId;
    private String version;
    private String relativePath;

    public Parent() {}

    public Parent(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "artifactId": {
                    this.artifactId = xml.getElementText();
                    break;
                }
                case "groupId": {
                    this.groupId = xml.getElementText();
                    break;
                }
                case "version": {
                    this.version = xml.getElementText();
                    break;
                }
                case "relativePath": {
                    this.relativePath = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public Parent(Parent parent1, Parent parent2) {
        artifactId = parent2.artifactId == null ? parent1.artifactId : parent2.artifactId;
        groupId = parent2.groupId == null ? parent1.groupId : parent2.groupId;
        version = parent2.version == null ? parent1.version : parent2.version;
        relativePath = parent2.relativePath == null ? parent1.relativePath : parent2.relativePath;
    }

    public void transform(Transformer transformer) {
        artifactId = transformer.transform(artifactId);
        groupId = transformer.transform(groupId);
        version = transformer.transform(version);
        relativePath = transformer.transform(relativePath);
    }


    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public String getRelativePath() {
        return relativePath;
    }
}

