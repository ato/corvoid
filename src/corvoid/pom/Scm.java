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
    private String tag;
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

    public Scm(Scm scm1, Scm scm2) {
        connection = scm2.connection == null ? scm1.connection : scm2.connection;
        developerConnection = scm2.developerConnection == null ? scm1.developerConnection : scm2.developerConnection;
        tag = scm2.tag == null ? scm1.tag : scm2.tag;
        url = scm2.url == null ? scm1.url : scm2.url;
    }

    public void transform(Transformer transformer) {
        connection = transformer.transform(connection);
        developerConnection = transformer.transform(developerConnection);
        tag = transformer.transform(tag);
        url = transformer.transform(url);
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

