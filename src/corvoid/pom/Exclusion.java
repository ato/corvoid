package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Exclusion {
    private String artifactId;
    private String groupId;

    public Exclusion() {}

    public Exclusion(XMLStreamReader xml) throws XMLStreamException {
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
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public Exclusion(Exclusion exclusion1, Exclusion exclusion2) {
        artifactId = exclusion2.artifactId == null ? exclusion1.artifactId : exclusion2.artifactId;
        groupId = exclusion2.groupId == null ? exclusion1.groupId : exclusion2.groupId;
    }

    public void transform(Transformer transformer) {
        artifactId = transformer.transform(artifactId);
        groupId = transformer.transform(groupId);
    }


    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }
}

