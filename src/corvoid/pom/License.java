package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class License {
    private String name;
    private String url;
    private String distribution;
    private String comments;

    public License() {}

    public License(XMLStreamReader xml) throws XMLStreamException {
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
                case "distribution": {
                    this.distribution = xml.getElementText();
                    break;
                }
                case "comments": {
                    this.comments = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public License(License license1, License license2) {
        name = license2.name == null ? license1.name : license2.name;
        url = license2.url == null ? license1.url : license2.url;
        distribution = license2.distribution == null ? license1.distribution : license2.distribution;
        comments = license2.comments == null ? license1.comments : license2.comments;
    }

    public void transform(Transformer transformer) {
        name = transformer.transform(name);
        url = transformer.transform(url);
        distribution = transformer.transform(distribution);
        comments = transformer.transform(comments);
    }


    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getComments() {
        return comments;
    }
}

