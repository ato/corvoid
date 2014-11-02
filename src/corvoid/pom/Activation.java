package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Activation {
    private Boolean activeByDefault;
    private String jdk;
    private ActivationOS os = new ActivationOS();
    private ActivationProperty property = new ActivationProperty();
    private ActivationFile file = new ActivationFile();

    public Activation() {}

    public Activation(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "activeByDefault": {
                    this.activeByDefault = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "jdk": {
                    this.jdk = xml.getElementText();
                    break;
                }
                case "os": {
                    this.os = new ActivationOS(xml);
                    break;
                }
                case "property": {
                    this.property = new ActivationProperty(xml);
                    break;
                }
                case "file": {
                    this.file = new ActivationFile(xml);
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public Activation(Activation activation1, Activation activation2) {
        activeByDefault = activation2.activeByDefault == null ? activation1.activeByDefault : activation2.activeByDefault;
        jdk = activation2.jdk == null ? activation1.jdk : activation2.jdk;
        os = new ActivationOS(activation1.os, activation2.os);
        property = new ActivationProperty(activation1.property, activation2.property);
        file = new ActivationFile(activation1.file, activation2.file);
    }

    public void transform(Transformer transformer) {
        jdk = transformer.transform(jdk);
        os.transform(transformer);
        property.transform(transformer);
        file.transform(transformer);
    }


    public Boolean getActiveByDefault() {
        return activeByDefault;
    }

    public String getJdk() {
        return jdk;
    }

    public ActivationOS getOs() {
        return os;
    }

    public ActivationProperty getProperty() {
        return property;
    }

    public ActivationFile getFile() {
        return file;
    }
}

