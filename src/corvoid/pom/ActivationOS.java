package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class ActivationOS {
    private String name;
    private String family;
    private String arch;
    private String version;

    public ActivationOS() {}

    public ActivationOS(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "name": {
                    this.name = xml.getElementText();
                    break;
                }
                case "family": {
                    this.family = xml.getElementText();
                    break;
                }
                case "arch": {
                    this.arch = xml.getElementText();
                    break;
                }
                case "version": {
                    this.version = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return family;
    }

    public String getArch() {
        return arch;
    }

    public String getVersion() {
        return version;
    }
}

