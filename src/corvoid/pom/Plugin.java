package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Plugin {
    private String groupId = "org.apache.maven.plugins";
    private String artifactId;
    private String version;
    private boolean extensions = false;
    private List<PluginExecution> executions = new ArrayList<>();
    private List<Dependency> dependencies = new ArrayList<>();
    private Map<String,String> goals = new HashMap<>();
    private String inherited;
    private Map<String,String> configuration = new HashMap<>();

    public Plugin() {}

    public Plugin(XMLStreamReader xml) throws XMLStreamException {
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
                case "extensions": {
                    this.extensions = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "executions": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("execution")) {
                            this.executions.add(new PluginExecution(xml));
                        } else {
                            throw new XMLStreamException("Expected <execution> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "dependencies": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("dependency")) {
                            this.dependencies.add(new Dependency(xml));
                        } else {
                            throw new XMLStreamException("Expected <dependency> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "goals": {
                    for (int depth = 1; depth > 0;) {
                        if (xml.next() == START_ELEMENT) {
                            depth++;
                        } else if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                            depth--;
                        }
                    }
                    break;
                }
                case "inherited": {
                    this.inherited = xml.getElementText();
                    break;
                }
                case "configuration": {
                    for (int depth = 1; depth > 0;) {
                        if (xml.next() == START_ELEMENT) {
                            depth++;
                        } else if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                            depth--;
                        }
                    }
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

    public boolean isExtensions() {
        return extensions;
    }

    public List<PluginExecution> getExecutions() {
        return executions;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public Map<String,String> getGoals() {
        return goals;
    }

    public String getInherited() {
        return inherited;
    }

    public Map<String,String> getConfiguration() {
        return configuration;
    }
}

