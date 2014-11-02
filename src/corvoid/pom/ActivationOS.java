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

    public ActivationOS(ActivationOS activationOS1, ActivationOS activationOS2) {
        name = activationOS2.name == null ? activationOS1.name : activationOS2.name;
        family = activationOS2.family == null ? activationOS1.family : activationOS2.family;
        arch = activationOS2.arch == null ? activationOS1.arch : activationOS2.arch;
        version = activationOS2.version == null ? activationOS1.version : activationOS2.version;
    }

    public void transform(Transformer transformer) {
        name = transformer.transform(name);
        family = transformer.transform(family);
        arch = transformer.transform(arch);
        version = transformer.transform(version);
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

