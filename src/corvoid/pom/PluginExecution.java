package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class PluginExecution {
    private String id;
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

    public PluginExecution(PluginExecution pluginExecution1, PluginExecution pluginExecution2) {
        id = pluginExecution2.id == null ? pluginExecution1.id : pluginExecution2.id;
        phase = pluginExecution2.phase == null ? pluginExecution1.phase : pluginExecution2.phase;
        goals.addAll(pluginExecution1.goals);
        goals.addAll(pluginExecution2.goals);
        inherited = pluginExecution2.inherited == null ? pluginExecution1.inherited : pluginExecution2.inherited;
        configuration.putAll(pluginExecution1.configuration);
        configuration.putAll(pluginExecution2.configuration);
    }

    public void transform(Transformer transformer) {
        id = transformer.transform(id);
        phase = transformer.transform(phase);
        for (int i = 0; i < goals.size(); i++) {
            goals.set(i, transformer.transform(goals.get(i)));
        }
        inherited = transformer.transform(inherited);
        for (String key: configuration.keySet()) {
            configuration.put(key, transformer.transform(configuration.get(key)));
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

