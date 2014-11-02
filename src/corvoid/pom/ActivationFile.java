package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class ActivationFile {
    private String missing;
    private String exists;

    public ActivationFile() {}

    public ActivationFile(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "missing": {
                    this.missing = xml.getElementText();
                    break;
                }
                case "exists": {
                    this.exists = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public ActivationFile(ActivationFile activationFile1, ActivationFile activationFile2) {
        missing = activationFile2.missing == null ? activationFile1.missing : activationFile2.missing;
        exists = activationFile2.exists == null ? activationFile1.exists : activationFile2.exists;
    }

    public void transform(Transformer transformer) {
        missing = transformer.transform(missing);
        exists = transformer.transform(exists);
    }


    public String getMissing() {
        return missing;
    }

    public String getExists() {
        return exists;
    }
}

