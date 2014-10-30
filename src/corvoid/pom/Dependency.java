package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Dependency {
    private String groupId;
    private String artifactId;
    private String version;
    private String type = "jar";
    private String classifier;
    private String scope;
    private String systemPath;
    private List<Exclusion> exclusions = new ArrayList<>();
    private boolean optional = false;

    public Dependency() {}

    public Dependency(XMLStreamReader xml) throws XMLStreamException {
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
                case "type": {
                    this.type = xml.getElementText();
                    break;
                }
                case "classifier": {
                    this.classifier = xml.getElementText();
                    break;
                }
                case "scope": {
                    this.scope = xml.getElementText();
                    break;
                }
                case "systemPath": {
                    this.systemPath = xml.getElementText();
                    break;
                }
                case "exclusions": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("exclusion")) {
                            this.exclusions.add(new Exclusion(xml));
                        } else {
                            throw new XMLStreamException("Expected <exclusion> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "optional": {
                    this.optional = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
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

    public String getType() {
        return type;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getScope() {
        return scope;
    }

    public String getSystemPath() {
        return systemPath;
    }

    public List<Exclusion> getExclusions() {
        return exclusions;
    }

    public boolean isOptional() {
        return optional;
    }
}

