package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class CiManagement {
    private String system;
    private String url;
    private List<Notifier> notifiers = new ArrayList<>();

    public CiManagement() {}

    public CiManagement(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "system": {
                    this.system = xml.getElementText();
                    break;
                }
                case "url": {
                    this.url = xml.getElementText();
                    break;
                }
                case "notifiers": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("notifier")) {
                            this.notifiers.add(new Notifier(xml));
                        } else {
                            throw new XMLStreamException("Expected <notifier> but got: " + xml.getLocalName(), xml.getLocation());
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

    public CiManagement(CiManagement ciManagement1, CiManagement ciManagement2) {
        system = ciManagement2.system == null ? ciManagement1.system : ciManagement2.system;
        url = ciManagement2.url == null ? ciManagement1.url : ciManagement2.url;
        notifiers.addAll(ciManagement1.notifiers);
        notifiers.addAll(ciManagement2.notifiers);
    }

    public void transform(Transformer transformer) {
        system = transformer.transform(system);
        url = transformer.transform(url);
        for (int i = 0; i < notifiers.size(); i++) {
            notifiers.get(i).transform(transformer);
        }
    }


    public String getSystem() {
        return system;
    }

    public String getUrl() {
        return url;
    }

    public List<Notifier> getNotifiers() {
        return notifiers;
    }
}

