package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Profile {
    private String id;
    private Activation activation = new Activation();
    private BuildBase build = new BuildBase();
    private List<String> modules = new ArrayList<>();
    private List<Repository> repositories = new ArrayList<>();
    private List<Repository> pluginRepositories = new ArrayList<>();
    private List<Dependency> dependencies = new ArrayList<>();
    private Map<String,String> reports = new HashMap<>();
    private Reporting reporting = new Reporting();
    private DependencyManagement dependencyManagement = new DependencyManagement();
    private DistributionManagement distributionManagement = new DistributionManagement();
    private Map<String,String> properties = new HashMap<>();

    public Profile() {}

    public Profile(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "id": {
                    this.id = xml.getElementText();
                    break;
                }
                case "activation": {
                    this.activation = new Activation(xml);
                    break;
                }
                case "build": {
                    this.build = new BuildBase(xml);
                    break;
                }
                case "modules": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("module")) {
                            this.modules.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <module> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "repositories": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("repository")) {
                            this.repositories.add(new Repository(xml));
                        } else {
                            throw new XMLStreamException("Expected <repository> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "pluginRepositories": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("pluginRepository")) {
                            this.pluginRepositories.add(new Repository(xml));
                        } else {
                            throw new XMLStreamException("Expected <pluginRepository> but got: " + xml.getLocalName(), xml.getLocation());
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
                case "reports": {
                    for (int depth = 1; depth > 0;) {
                        if (xml.next() == START_ELEMENT) {
                            depth++;
                        } else if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                            depth--;
                        }
                    }
                    break;
                }
                case "reporting": {
                    this.reporting = new Reporting(xml);
                    break;
                }
                case "dependencyManagement": {
                    this.dependencyManagement = new DependencyManagement(xml);
                    break;
                }
                case "distributionManagement": {
                    this.distributionManagement = new DistributionManagement(xml);
                    break;
                }
                case "properties": {
                    while (xml.nextTag() == START_ELEMENT) {
                        this.properties.put(xml.getLocalName(), xml.getElementText());
                    }
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public Activation getActivation() {
        return activation;
    }

    public BuildBase getBuild() {
        return build;
    }

    public List<String> getModules() {
        return modules;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public List<Repository> getPluginRepositories() {
        return pluginRepositories;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public Map<String,String> getReports() {
        return reports;
    }

    public Reporting getReporting() {
        return reporting;
    }

    public DependencyManagement getDependencyManagement() {
        return dependencyManagement;
    }

    public DistributionManagement getDistributionManagement() {
        return distributionManagement;
    }

    public Map<String,String> getProperties() {
        return properties;
    }
}

