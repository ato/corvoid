package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class PluginExecution {
    private String id = "default";
    private String phase;
    private List<String> goals = new ArrayList<>();
    private String inherited;
    private Map<String,String> configuration = new HashMap<>();

    public PluginExecution() {}

    public PluginExecution(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "id": {
                    this.id = xml.getElementText();
                    break;
                }
                case "phase": {
                    this.phase = xml.getElementText();
                    break;
                }
                case "goals": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("goal")) {
                            this.goals.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <goal> but got: " + xml.getLocalName(), xml.getLocation());
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

    public String getId() {
        return id;
    }

    public String getPhase() {
        return phase;
    }

    public List<String> getGoals() {
        return goals;
    }

    public String getInherited() {
        return inherited;
    }

    public Map<String,String> getConfiguration() {
        return configuration;
    }
}

