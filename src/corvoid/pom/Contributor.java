package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Contributor {
    private String name;
    private String email;
    private String url;
    private String organization;
    private String organizationUrl;
    private List<String> roles = new ArrayList<>();
    private String timezone;
    private Map<String,String> properties = new HashMap<>();

    public Contributor() {}

    public Contributor(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
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

    public Contributor(Contributor contributor1, Contributor contributor2) {
        name = contributor2.name == null ? contributor1.name : contributor2.name;
        email = contributor2.email == null ? contributor1.email : contributor2.email;
        url = contributor2.url == null ? contributor1.url : contributor2.url;
        organization = contributor2.organization == null ? contributor1.organization : contributor2.organization;
        organizationUrl = contributor2.organizationUrl == null ? contributor1.organizationUrl : contributor2.organizationUrl;
        roles.addAll(contributor1.roles);
        roles.addAll(contributor2.roles);
        timezone = contributor2.timezone == null ? contributor1.timezone : contributor2.timezone;
        properties.putAll(contributor1.properties);
        properties.putAll(contributor2.properties);
    }

    public void transform(Transformer transformer) {
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

