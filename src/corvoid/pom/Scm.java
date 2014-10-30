package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Scm {
    private String connection;
    private String developerConnection;
    private String tag = "HEAD";
    private String url;

    public Scm() {}

    public Scm(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "connection": {
                    this.connection = xml.getElementText();
                    break;
                }
                case "developerConnection": {
                    this.developerConnection = xml.getElementText();
                    break;
                }
                case "tag": {
                    this.tag = xml.getElementText();
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

    public String getConnection() {
        return connection;
    }

    public String getDeveloperConnection() {
        return developerConnection;
    }

    public String getTag() {
        return tag;
    }

    public String getUrl() {
        return url;
    }
}

