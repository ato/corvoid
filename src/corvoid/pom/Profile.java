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

    public Profile(Profile profile1, Profile profile2) {
        id = profile2.id == null ? profile1.id : profile2.id;
        activation = new Activation(profile1.activation, profile2.activation);
        build = new BuildBase(profile1.build, profile2.build);
        modules.addAll(profile1.modules);
        modules.addAll(profile2.modules);
        repositories.addAll(profile1.repositories);
        repositories.addAll(profile2.repositories);
        pluginRepositories.addAll(profile1.pluginRepositories);
        pluginRepositories.addAll(profile2.pluginRepositories);
        dependencies.addAll(profile1.dependencies);
        dependencies.addAll(profile2.dependencies);
        reports.putAll(profile1.reports);
        reports.putAll(profile2.reports);
        reporting = new Reporting(profile1.reporting, profile2.reporting);
        dependencyManagement = new DependencyManagement(profile1.dependencyManagement, profile2.dependencyManagement);
        distributionManagement = new DistributionManagement(profile1.distributionManagement, profile2.distributionManagement);
        properties.putAll(profile1.properties);
        properties.putAll(profile2.properties);
    }

    public void transform(Transformer transformer) {
        id = transformer.transform(id);
        activation.transform(transformer);
        build.transform(transformer);
        for (int i = 0; i < modules.size(); i++) {
            modules.set(i, transformer.transform(modules.get(i)));
        }
        for (int i = 0; i < repositories.size(); i++) {
            repositories.get(i).transform(transformer);
        }
        for (int i = 0; i < pluginRepositories.size(); i++) {
            pluginRepositories.get(i).transform(transformer);
        }
        for (int i = 0; i < dependencies.size(); i++) {
            dependencies.get(i).transform(transformer);
        }
        for (String key: reports.keySet()) {
            reports.put(key, transformer.transform(reports.get(key)));
        }
        reporting.transform(transformer);
        dependencyManagement.transform(transformer);
        distributionManagement.transform(transformer);
        for (String key: properties.keySet()) {
            properties.put(key, transformer.transform(properties.get(key)));
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

