package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Reporting {
    private boolean excludeDefaults = false;
    private String outputDirectory;
    private List<ReportPlugin> plugins = new ArrayList<>();

    public Reporting() {}

    public Reporting(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "excludeDefaults": {
                    this.excludeDefaults = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "outputDirectory": {
                    this.outputDirectory = xml.getElementText();
                    break;
                }
                case "plugins": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("plugin")) {
                            this.plugins.add(new ReportPlugin(xml));
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

    public boolean isExcludeDefaults() {
        return excludeDefaults;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public List<ReportPlugin> getPlugins() {
        return plugins;
    }
}

