package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class DeploymentRepository {
    private Boolean uniqueVersion;
    private String id;
    private String name;
    private String url;
    private String layout;

    public DeploymentRepository() {}

    public DeploymentRepository(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "uniqueVersion": {
                    this.uniqueVersion = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
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
                case "layout": {
                    this.layout = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public DeploymentRepository(DeploymentRepository deploymentRepository1, DeploymentRepository deploymentRepository2) {
        uniqueVersion = deploymentRepository2.uniqueVersion == null ? deploymentRepository1.uniqueVersion : deploymentRepository2.uniqueVersion;
        id = deploymentRepository2.id == null ? deploymentRepository1.id : deploymentRepository2.id;
        name = deploymentRepository2.name == null ? deploymentRepository1.name : deploymentRepository2.name;
        url = deploymentRepository2.url == null ? deploymentRepository1.url : deploymentRepository2.url;
        layout = deploymentRepository2.layout == null ? deploymentRepository1.layout : deploymentRepository2.layout;
    }

    public void transform(Transformer transformer) {
        id = transformer.transform(id);
        name = transformer.transform(name);
        url = transformer.transform(url);
        layout = transformer.transform(layout);
    }


    public Boolean getUniqueVersion() {
        return uniqueVersion;
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

    public String getLayout() {
        return layout;
    }
}

