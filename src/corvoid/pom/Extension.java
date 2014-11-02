package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Extension {
    private String groupId;
    private String artifactId;
    private String version;

    public Extension() {}

    public Extension(XMLStreamReader xml) throws XMLStreamException {
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
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public Extension(Extension extension1, Extension extension2) {
        groupId = extension2.groupId == null ? extension1.groupId : extension2.groupId;
        artifactId = extension2.artifactId == null ? extension1.artifactId : extension2.artifactId;
        version = extension2.version == null ? extension1.version : extension2.version;
    }

    public void transform(Transformer transformer) {
        groupId = transformer.transform(groupId);
        artifactId = transformer.transform(artifactId);
        version = transformer.transform(version);
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
}

