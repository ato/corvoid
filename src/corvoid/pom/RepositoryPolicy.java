package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class RepositoryPolicy {
    private Boolean enabled;
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

    public RepositoryPolicy(RepositoryPolicy repositoryPolicy1, RepositoryPolicy repositoryPolicy2) {
        enabled = repositoryPolicy2.enabled == null ? repositoryPolicy1.enabled : repositoryPolicy2.enabled;
        updatePolicy = repositoryPolicy2.updatePolicy == null ? repositoryPolicy1.updatePolicy : repositoryPolicy2.updatePolicy;
        checksumPolicy = repositoryPolicy2.checksumPolicy == null ? repositoryPolicy1.checksumPolicy : repositoryPolicy2.checksumPolicy;
    }

    public void transform(Transformer transformer) {
        updatePolicy = transformer.transform(updatePolicy);
        checksumPolicy = transformer.transform(checksumPolicy);
    }


    public Boolean getEnabled() {
        return enabled;
    }

    public String getUpdatePolicy() {
        return updatePolicy;
    }

    public String getChecksumPolicy() {
        return checksumPolicy;
    }
}

