package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class RepositoryPolicy {
    private boolean enabled = true;
    private String updatePolicy;
    private String checksumPolicy;

    public RepositoryPolicy() {}

    public RepositoryPolicy(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "enabled": {
                    this.enabled = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "updatePolicy": {
                    this.updatePolicy = xml.getElementText();
                    break;
                }
                case "checksumPolicy": {
                    this.checksumPolicy = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getUpdatePolicy() {
        return updatePolicy;
    }

    public String getChecksumPolicy() {
        return checksumPolicy;
    }
}

