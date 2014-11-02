package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class IssueManagement {
    private String system;
    private String url;

    public IssueManagement() {}

    public IssueManagement(XMLStreamReader xml) throws XMLStreamException {
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
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public IssueManagement(IssueManagement issueManagement1, IssueManagement issueManagement2) {
        system = issueManagement2.system == null ? issueManagement1.system : issueManagement2.system;
        url = issueManagement2.url == null ? issueManagement1.url : issueManagement2.url;
    }

    public void transform(Transformer transformer) {
        system = transformer.transform(system);
        url = transformer.transform(url);
    }


    public String getSystem() {
        return system;
    }

    public String getUrl() {
        return url;
    }
}

