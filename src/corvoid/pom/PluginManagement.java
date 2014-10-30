package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class PluginManagement {
    private List<Plugin> plugins = new ArrayList<>();

    public PluginManagement() {}

    public PluginManagement(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
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

    public List<Plugin> getPlugins() {
        return plugins;
    }
}

