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

    public Developer(Developer developer1, Developer developer2) {
        id = developer2.id == null ? developer1.id : developer2.id;
        name = developer2.name == null ? developer1.name : developer2.name;
        email = developer2.email == null ? developer1.email : developer2.email;
        url = developer2.url == null ? developer1.url : developer2.url;
        organization = developer2.organization == null ? developer1.organization : developer2.organization;
        organizationUrl = developer2.organizationUrl == null ? developer1.organizationUrl : developer2.organizationUrl;
        roles.addAll(developer1.roles);
        roles.addAll(developer2.roles);
        timezone = developer2.timezone == null ? developer1.timezone : developer2.timezone;
        properties.putAll(developer1.properties);
        properties.putAll(developer2.properties);
    }

    public void transform(Transformer transformer) {
        id = transformer.transform(id);
        name = transformer.transform(name);
        email = transformer.transform(email);
        url = transformer.transform(url);
        organization = transformer.transform(organization);
        organizationUrl = transformer.transform(organizationUrl);
        for (int i = 0; i < roles.size(); i++) {
            roles.set(i, transformer.transform(roles.get(i)));
        }
        timezone = transformer.transform(timezone);
        for (String key: properties.keySet()) {
            properties.put(key, transformer.transform(properties.get(key)));
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

