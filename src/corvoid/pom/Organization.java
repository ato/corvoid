package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Organization {
    private String name;
    private String url;

    public Organization() {}

    public Organization(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "name": {
                    this.name = xml.getElementText();
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

    public Organization(Organization organization1, Organization organization2) {
        name = organization2.name == null ? organization1.name : organization2.name;
        url = organization2.url == null ? organization1.url : organization2.url;
    }

    public void transform(Transformer transformer) {
        name = transformer.transform(name);
        url = transformer.transform(url);
    }


    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}

