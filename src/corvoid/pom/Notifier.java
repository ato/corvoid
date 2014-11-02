package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Notifier {
    private String type;
    private Boolean sendOnError;
    private Boolean sendOnFailure;
    private Boolean sendOnSuccess;
    private Boolean sendOnWarning;
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

    public Notifier(Notifier notifier1, Notifier notifier2) {
        type = notifier2.type == null ? notifier1.type : notifier2.type;
        sendOnError = notifier2.sendOnError == null ? notifier1.sendOnError : notifier2.sendOnError;
        sendOnFailure = notifier2.sendOnFailure == null ? notifier1.sendOnFailure : notifier2.sendOnFailure;
        sendOnSuccess = notifier2.sendOnSuccess == null ? notifier1.sendOnSuccess : notifier2.sendOnSuccess;
        sendOnWarning = notifier2.sendOnWarning == null ? notifier1.sendOnWarning : notifier2.sendOnWarning;
        address = notifier2.address == null ? notifier1.address : notifier2.address;
        configuration.putAll(notifier1.configuration);
        configuration.putAll(notifier2.configuration);
    }

    public void transform(Transformer transformer) {
        type = transformer.transform(type);
        address = transformer.transform(address);
        for (String key: configuration.keySet()) {
            configuration.put(key, transformer.transform(configuration.get(key)));
        }
    }


    public String getType() {
        return type;
    }

    public Boolean getSendOnError() {
        return sendOnError;
    }

    public Boolean getSendOnFailure() {
        return sendOnFailure;
    }

    public Boolean getSendOnSuccess() {
        return sendOnSuccess;
    }

    public Boolean getSendOnWarning() {
        return sendOnWarning;
    }

    public String getAddress() {
        return address;
    }

    public Map<String,String> getConfiguration() {
        return configuration;
    }
}

