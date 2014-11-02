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

    public DistributionManagement(DistributionManagement distributionManagement1, DistributionManagement distributionManagement2) {
        repository = new DeploymentRepository(distributionManagement1.repository, distributionManagement2.repository);
        snapshotRepository = new DeploymentRepository(distributionManagement1.snapshotRepository, distributionManagement2.snapshotRepository);
        site = new Site(distributionManagement1.site, distributionManagement2.site);
        downloadUrl = distributionManagement2.downloadUrl == null ? distributionManagement1.downloadUrl : distributionManagement2.downloadUrl;
        relocation = new Relocation(distributionManagement1.relocation, distributionManagement2.relocation);
        status = distributionManagement2.status == null ? distributionManagement1.status : distributionManagement2.status;
    }

    public void transform(Transformer transformer) {
        repository.transform(transformer);
        snapshotRepository.transform(transformer);
        site.transform(transformer);
        downloadUrl = transformer.transform(downloadUrl);
        relocation.transform(transformer);
        status = transformer.transform(status);
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

