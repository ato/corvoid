package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Relocation {
    private String groupId;
    private String artifactId;
    private String version;
    private String message;

    public Relocation() {}

    public Relocation(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "groupId": {
                    this.groupId = xml.getElementText();
                    break;
                }
                case "artifactId": {
                    this.artifactId = xml.getElementText();
                    break;
                }
                case "version": {
                    this.version = xml.getElementText();
                    break;
                }
                case "message": {
                    this.message = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public Relocation(Relocation relocation1, Relocation relocation2) {
        groupId = relocation2.groupId == null ? relocation1.groupId : relocation2.groupId;
        artifactId = relocation2.artifactId == null ? relocation1.artifactId : relocation2.artifactId;
        version = relocation2.version == null ? relocation1.version : relocation2.version;
        message = relocation2.message == null ? relocation1.message : relocation2.message;
    }

    public void transform(Transformer transformer) {
        groupId = transformer.transform(groupId);
        artifactId = transformer.transform(artifactId);
        version = transformer.transform(version);
        message = transformer.transform(message);
    }


    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getMessage() {
        return message;
    }
}

