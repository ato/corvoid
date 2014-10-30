package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Repository {
    private RepositoryPolicy releases = new RepositoryPolicy();
    private RepositoryPolicy snapshots = new RepositoryPolicy();
    private String id;
    private String name;
    private String url;
    private String layout = "default";

    public Repository() {}

    public Repository(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "releases": {
                    this.releases = new RepositoryPolicy(xml);
                    break;
                }
                case "snapshots": {
                    this.snapshots = new RepositoryPolicy(xml);
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

    public RepositoryPolicy getReleases() {
        return releases;
    }

    public RepositoryPolicy getSnapshots() {
        return snapshots;
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

