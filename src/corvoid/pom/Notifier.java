package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Notifier {
    private String type = "mail";
    private boolean sendOnError = true;
    private boolean sendOnFailure = true;
    private boolean sendOnSuccess = true;
    private boolean sendOnWarning = true;
    private String address;
    private Map<String,String> configuration = new HashMap<>();

    public Notifier() {}

    public Notifier(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "type": {
                    this.type = xml.getElementText();
                    break;
                }
                case "sendOnError": {
                    this.sendOnError = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "sendOnFailure": {
                    this.sendOnFailure = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "sendOnSuccess": {
                    this.sendOnSuccess = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "sendOnWarning": {
                    this.sendOnWarning = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "address": {
                    this.address = xml.getElementText();
                    break;
                }
                case "configuration": {
                    for (int depth = 1; depth > 0;) {
                        if (xml.next() == START_ELEMENT) {
                            depth++;
                        } else if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                            depth--;
                        }
                    }
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public String getType() {
        return type;
    }

    public boolean isSendOnError() {
        return sendOnError;
    }

    public boolean isSendOnFailure() {
        return sendOnFailure;
    }

    public boolean isSendOnSuccess() {
        return sendOnSuccess;
    }

    public boolean isSendOnWarning() {
        return sendOnWarning;
    }

    public String getAddress() {
        return address;
    }

    public Map<String,String> getConfiguration() {
        return configuration;
    }
}

