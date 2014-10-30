package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class ReportSet {
    private String id = "default";
    private Map<String,String> configuration = new HashMap<>();
    private String inherited;
    private List<String> reports = new ArrayList<>();

    public ReportSet() {}

    public ReportSet(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "id": {
                    this.id = xml.getElementText();
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
                case "inherited": {
                    this.inherited = xml.getElementText();
                    break;
                }
                case "reports": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("report")) {
                            this.reports.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <report> but got: " + xml.getLocalName(), xml.getLocation());
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

    public Map<String,String> getConfiguration() {
        return configuration;
    }

    public String getInherited() {
        return inherited;
    }

    public List<String> getReports() {
        return reports;
    }
}

