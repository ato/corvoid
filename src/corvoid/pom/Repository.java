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
    private String layout;

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

    public Repository(Repository repository1, Repository repository2) {
        releases = new RepositoryPolicy(repository1.releases, repository2.releases);
        snapshots = new RepositoryPolicy(repository1.snapshots, repository2.snapshots);
        id = repository2.id == null ? repository1.id : repository2.id;
        name = repository2.name == null ? repository1.name : repository2.name;
        url = repository2.url == null ? repository1.url : repository2.url;
        layout = repository2.layout == null ? repository1.layout : repository2.layout;
    }

    public void transform(Transformer transformer) {
        releases.transform(transformer);
        snapshots.transform(transformer);
        id = transformer.transform(id);
        name = transformer.transform(name);
        url = transformer.transform(url);
        layout = transformer.transform(layout);
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

