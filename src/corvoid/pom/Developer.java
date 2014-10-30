package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Developer {
    private String id;
    private String name;
    private String email;
    private String url;
    private String organization;
    private String organizationUrl;
    private List<String> roles = new ArrayList<>();
    private String timezone;
    private Map<String,String> properties = new HashMap<>();

    public Developer() {}

    public Developer(XMLStreamReader xml) throws XMLStreamException {
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
                case "email": {
                    this.email = xml.getElementText();
                    break;
                }
                case "url": {
                    this.url = xml.getElementText();
                    break;
                }
                case "organization": {
                    this.organization = xml.getElementText();
                    break;
                }
                case "organizationUrl": {
                    this.organizationUrl = xml.getElementText();
                    break;
                }
                case "roles": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("role")) {
                            this.roles.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <role> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "timezone": {
                    this.timezone = xml.getElementText();
                    break;
                }
                case "properties": {
                    while (xml.nextTag() == START_ELEMENT) {
                        this.properties.put(xml.getLocalName(), xml.getElementText());
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

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getUrl() {
        return url;
    }

    public String getOrganization() {
        return organization;
    }

    public String getOrganizationUrl() {
        return organizationUrl;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getTimezone() {
        return timezone;
    }

    public Map<String,String> getProperties() {
        return properties;
    }
}

