package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Prerequisites {
    private String maven;

    public Prerequisites() {}

    public Prerequisites(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "maven": {
                    this.maven = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public Prerequisites(Prerequisites prerequisites1, Prerequisites prerequisites2) {
        maven = prerequisites2.maven == null ? prerequisites1.maven : prerequisites2.maven;
    }

    public void transform(Transformer transformer) {
        maven = transformer.transform(maven);
    }


    public String getMaven() {
        return maven;
    }
}

