package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class MailingList {
    private String name;
    private String subscribe;
    private String unsubscribe;
    private String post;
    private String archive;
    private List<String> otherArchives = new ArrayList<>();

    public MailingList() {}

    public MailingList(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "name": {
                    this.name = xml.getElementText();
                    break;
                }
                case "subscribe": {
                    this.subscribe = xml.getElementText();
                    break;
                }
                case "unsubscribe": {
                    this.unsubscribe = xml.getElementText();
                    break;
                }
                case "post": {
                    this.post = xml.getElementText();
                    break;
                }
                case "archive": {
                    this.archive = xml.getElementText();
                    break;
                }
                case "otherArchives": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("otherArchive")) {
                            this.otherArchives.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <otherArchive> but got: " + xml.getLocalName(), xml.getLocation());
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

    public MailingList(MailingList mailingList1, MailingList mailingList2) {
        name = mailingList2.name == null ? mailingList1.name : mailingList2.name;
        subscribe = mailingList2.subscribe == null ? mailingList1.subscribe : mailingList2.subscribe;
        unsubscribe = mailingList2.unsubscribe == null ? mailingList1.unsubscribe : mailingList2.unsubscribe;
        post = mailingList2.post == null ? mailingList1.post : mailingList2.post;
        archive = mailingList2.archive == null ? mailingList1.archive : mailingList2.archive;
        otherArchives.addAll(mailingList1.otherArchives);
        otherArchives.addAll(mailingList2.otherArchives);
    }

    public void transform(Transformer transformer) {
        name = transformer.transform(name);
        subscribe = transformer.transform(subscribe);
        unsubscribe = transformer.transform(unsubscribe);
        post = transformer.transform(post);
        archive = transformer.transform(archive);
        for (int i = 0; i < otherArchives.size(); i++) {
            otherArchives.set(i, transformer.transform(otherArchives.get(i)));
        }
    }


    public String getName() {
        return name;
    }

    public String getSubscribe() {
        return subscribe;
    }

    public String getUnsubscribe() {
        return unsubscribe;
    }

    public String getPost() {
        return post;
    }

    public String getArchive() {
        return archive;
    }

    public List<String> getOtherArchives() {
        return otherArchives;
    }
}

