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
    private String type;
    private String classifier;
    private String scope;
    private String systemPath;
    private List<Exclusion> exclusions = new ArrayList<>();
    private Boolean optional;

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

    public Dependency(Dependency dependency1, Dependency dependency2) {
        groupId = dependency2.groupId == null ? dependency1.groupId : dependency2.groupId;
        artifactId = dependency2.artifactId == null ? dependency1.artifactId : dependency2.artifactId;
        version = dependency2.version == null ? dependency1.version : dependency2.version;
        type = dependency2.type == null ? dependency1.type : dependency2.type;
        classifier = dependency2.classifier == null ? dependency1.classifier : dependency2.classifier;
        scope = dependency2.scope == null ? dependency1.scope : dependency2.scope;
        systemPath = dependency2.systemPath == null ? dependency1.systemPath : dependency2.systemPath;
        exclusions.addAll(dependency1.exclusions);
        exclusions.addAll(dependency2.exclusions);
        optional = dependency2.optional == null ? dependency1.optional : dependency2.optional;
    }

    public void transform(Transformer transformer) {
        groupId = transformer.transform(groupId);
        artifactId = transformer.transform(artifactId);
        version = transformer.transform(version);
        type = transformer.transform(type);
        classifier = transformer.transform(classifier);
        scope = transformer.transform(scope);
        systemPath = transformer.transform(systemPath);
        for (int i = 0; i < exclusions.size(); i++) {
            exclusions.get(i).transform(transformer);
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

    public Boolean getOptional() {
        return optional;
    }
}

