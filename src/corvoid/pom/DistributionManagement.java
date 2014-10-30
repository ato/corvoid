package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class DistributionManagement {
    private DeploymentRepository repository = new DeploymentRepository();
    private DeploymentRepository snapshotRepository = new DeploymentRepository();
    private Site site = new Site();
    private String downloadUrl;
    private Relocation relocation = new Relocation();
    private String status;

    public DistributionManagement() {}

    public DistributionManagement(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "repository": {
                    this.repository = new DeploymentRepository(xml);
                    break;
                }
                case "snapshotRepository": {
                    this.snapshotRepository = new DeploymentRepository(xml);
                    break;
                }
                case "site": {
                    this.site = new Site(xml);
                    break;
                }
                case "downloadUrl": {
                    this.downloadUrl = xml.getElementText();
                    break;
                }
                case "relocation": {
                    this.relocation = new Relocation(xml);
                    break;
                }
                case "status": {
                    this.status = xml.getElementText();
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public DeploymentRepository getRepository() {
        return repository;
    }

    public DeploymentRepository getSnapshotRepository() {
        return snapshotRepository;
    }

    public Site getSite() {
        return site;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public Relocation getRelocation() {
        return relocation;
    }

    public String getStatus() {
        return status;
    }
}

