package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class ActivationProperty {
    private String name;
    private String value;

    public ActivationProperty() {}

    public ActivationProperty(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "name": {
                    this.name = xml.getElementText();
                    break;
                }
                case "value": {
                    this.value = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public ActivationProperty(ActivationProperty activationProperty1, ActivationProperty activationProperty2) {
        name = activationProperty2.name == null ? activationProperty1.name : activationProperty2.name;
        value = activationProperty2.value == null ? activationProperty1.value : activationProperty2.value;
    }

    public void transform(Transformer transformer) {
        name = transformer.transform(name);
        value = transformer.transform(value);
    }


    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}

