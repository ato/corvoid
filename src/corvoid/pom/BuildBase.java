package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class BuildBase {
    private String defaultGoal;
    private List<Resource> resources = new ArrayList<>();
    private List<Resource> testResources = new ArrayList<>();
    private String directory;
    private String finalName;
    private List<String> filters = new ArrayList<>();
    private PluginManagement pluginManagement = new PluginManagement();
    private List<Plugin> plugins = new ArrayList<>();

    public BuildBase() {}

    public BuildBase(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "defaultGoal": {
                    this.defaultGoal = xml.getElementText();
                    break;
                }
                case "resources": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("resource")) {
                            this.resources.add(new Resource(xml));
                        } else {
                            throw new XMLStreamException("Expected <resource> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "testResources": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("testResource")) {
                            this.testResources.add(new Resource(xml));
                        } else {
                            throw new XMLStreamException("Expected <testResource> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "directory": {
                    this.directory = xml.getElementText();
                    break;
                }
                case "finalName": {
                    this.finalName = xml.getElementText();
                    break;
                }
                case "filters": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("filter")) {
                            this.filters.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <filter> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "pluginManagement": {
                    this.pluginManagement = new PluginManagement(xml);
                    break;
                }
                case "plugins": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("plugin")) {
                            this.plugins.add(new Plugin(xml));
                        } else {
                            throw new XMLStreamException("Expected <plugin> but got: " + xml.getLocalName(), xml.getLocation());
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

    public BuildBase(BuildBase buildBase1, BuildBase buildBase2) {
        defaultGoal = buildBase2.defaultGoal == null ? buildBase1.defaultGoal : buildBase2.defaultGoal;
        resources.addAll(buildBase1.resources);
        resources.addAll(buildBase2.resources);
        testResources.addAll(buildBase1.testResources);
        testResources.addAll(buildBase2.testResources);
        directory = buildBase2.directory == null ? buildBase1.directory : buildBase2.directory;
        finalName = buildBase2.finalName == null ? buildBase1.finalName : buildBase2.finalName;
        filters.addAll(buildBase1.filters);
        filters.addAll(buildBase2.filters);
        pluginManagement = new PluginManagement(buildBase1.pluginManagement, buildBase2.pluginManagement);
        plugins.addAll(buildBase1.plugins);
        plugins.addAll(buildBase2.plugins);
    }

    public void transform(Transformer transformer) {
        defaultGoal = transformer.transform(defaultGoal);
        for (int i = 0; i < resources.size(); i++) {
            resources.get(i).transform(transformer);
        }
        for (int i = 0; i < testResources.size(); i++) {
            testResources.get(i).transform(transformer);
        }
        directory = transformer.transform(directory);
        finalName = transformer.transform(finalName);
        for (int i = 0; i < filters.size(); i++) {
            filters.set(i, transformer.transform(filters.get(i)));
        }
        pluginManagement.transform(transformer);
        for (int i = 0; i < plugins.size(); i++) {
            plugins.get(i).transform(transformer);
        }
    }


    public String getDefaultGoal() {
        return defaultGoal;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public List<Resource> getTestResources() {
        return testResources;
    }

    public String getDirectory() {
        return directory;
    }

    public String getFinalName() {
        return finalName;
    }

    public List<String> getFilters() {
        return filters;
    }

    public PluginManagement getPluginManagement() {
        return pluginManagement;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }
}

