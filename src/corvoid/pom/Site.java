package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Site {
    private String id;
    private String name;
    private String url;

    public Site() {}

    public Site(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "id": {
                    this.id = xml.getElementText();
                    break;
                }
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

    public Site(Site site1, Site site2) {
        id = site2.id == null ? site1.id : site2.id;
        name = site2.name == null ? site1.name : site2.name;
        url = site2.url == null ? site1.url : site2.url;
    }

    public void transform(Transformer transformer) {
        id = transformer.transform(id);
        name = transformer.transform(name);
        url = transformer.transform(url);
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}

