package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Build {
    private String sourceDirectory;
    private String scriptSourceDirectory;
    private String testSourceDirectory;
    private String outputDirectory;
    private String testOutputDirectory;
    private List<Extension> extensions = new ArrayList<>();
    private String defaultGoal;
    private List<Resource> resources = new ArrayList<>();
    private List<Resource> testResources = new ArrayList<>();
    private String directory;
    private String finalName;
    private List<String> filters = new ArrayList<>();
    private PluginManagement pluginManagement = new PluginManagement();
    private List<Plugin> plugins = new ArrayList<>();

    public Build() {}

    public Build(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "sourceDirectory": {
                    this.sourceDirectory = xml.getElementText();
                    break;
                }
                case "scriptSourceDirectory": {
                    this.scriptSourceDirectory = xml.getElementText();
                    break;
                }
                case "testSourceDirectory": {
                    this.testSourceDirectory = xml.getElementText();
                    break;
                }
                case "outputDirectory": {
                    this.outputDirectory = xml.getElementText();
                    break;
                }
                case "testOutputDirectory": {
                    this.testOutputDirectory = xml.getElementText();
                    break;
                }
                case "extensions": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("extension")) {
                            this.extensions.add(new Extension(xml));
                        } else {
                            throw new XMLStreamException("Expected <extension> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
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

    public Build(Build build1, Build build2) {
        sourceDirectory = build2.sourceDirectory == null ? build1.sourceDirectory : build2.sourceDirectory;
        scriptSourceDirectory = build2.scriptSourceDirectory == null ? build1.scriptSourceDirectory : build2.scriptSourceDirectory;
        testSourceDirectory = build2.testSourceDirectory == null ? build1.testSourceDirectory : build2.testSourceDirectory;
        outputDirectory = build2.outputDirectory == null ? build1.outputDirectory : build2.outputDirectory;
        testOutputDirectory = build2.testOutputDirectory == null ? build1.testOutputDirectory : build2.testOutputDirectory;
        extensions.addAll(build1.extensions);
        extensions.addAll(build2.extensions);
        defaultGoal = build2.defaultGoal == null ? build1.defaultGoal : build2.defaultGoal;
        resources.addAll(build1.resources);
        resources.addAll(build2.resources);
        testResources.addAll(build1.testResources);
        testResources.addAll(build2.testResources);
        directory = build2.directory == null ? build1.directory : build2.directory;
        finalName = build2.finalName == null ? build1.finalName : build2.finalName;
        filters.addAll(build1.filters);
        filters.addAll(build2.filters);
        pluginManagement = new PluginManagement(build1.pluginManagement, build2.pluginManagement);
        plugins.addAll(build1.plugins);
        plugins.addAll(build2.plugins);
    }

    public void transform(Transformer transformer) {
        sourceDirectory = transformer.transform(sourceDirectory);
        scriptSourceDirectory = transformer.transform(scriptSourceDirectory);
        testSourceDirectory = transformer.transform(testSourceDirectory);
        outputDirectory = transformer.transform(outputDirectory);
        testOutputDirectory = transformer.transform(testOutputDirectory);
        for (int i = 0; i < extensions.size(); i++) {
            extensions.get(i).transform(transformer);
        }
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


    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getScriptSourceDirectory() {
        return scriptSourceDirectory;
    }

    public String getTestSourceDirectory() {
        return testSourceDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public String getTestOutputDirectory() {
        return testOutputDirectory;
    }

    public List<Extension> getExtensions() {
        return extensions;
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

