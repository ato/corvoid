package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Model {
    private Parent parent = new Parent();
    private String modelVersion;
    private String groupId;
    private String artifactId;
    private String packaging = "jar";
    private String name;
    private String version;
    private String description;
    private String url;
    private Prerequisites prerequisites = new Prerequisites();
    private IssueManagement issueManagement = new IssueManagement();
    private CiManagement ciManagement = new CiManagement();
    private String inceptionYear;
    private List<MailingList> mailingLists = new ArrayList<>();
    private List<Developer> developers = new ArrayList<>();
    private List<Contributor> contributors = new ArrayList<>();
    private List<License> licenses = new ArrayList<>();
    private Scm scm = new Scm();
    private Organization organization = new Organization();
    private Build build = new Build();
    private List<Profile> profiles = new ArrayList<>();
    private List<String> modules = new ArrayList<>();
    private List<Repository> repositories = new ArrayList<>();
    private List<Repository> pluginRepositories = new ArrayList<>();
    private List<Dependency> dependencies = new ArrayList<>();
    private Map<String,String> reports = new HashMap<>();
    private Reporting reporting = new Reporting();
    private DependencyManagement dependencyManagement = new DependencyManagement();
    private DistributionManagement distributionManagement = new DistributionManagement();
    private Map<String,String> properties = new HashMap<>();

    public Model() {}

    public Model(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "parent": {
                    this.parent = new Parent(xml);
                    break;
                }
                case "modelVersion": {
                    this.modelVersion = xml.getElementText();
                    break;
                }
                case "groupId": {
                    this.groupId = xml.getElementText();
                    break;
                }
                case "artifactId": {
                    this.artifactId = xml.getElementText();
                    break;
                }
                case "packaging": {
                    this.packaging = xml.getElementText();
                    break;
                }
                case "name": {
                    this.name = xml.getElementText();
                    break;
                }
                case "version": {
                    this.version = xml.getElementText();
                    break;
                }
                case "description": {
                    this.description = xml.getElementText();
                    break;
                }
                case "url": {
                    this.url = xml.getElementText();
                    break;
                }
                case "prerequisites": {
                    this.prerequisites = new Prerequisites(xml);
                    break;
                }
                case "issueManagement": {
                    this.issueManagement = new IssueManagement(xml);
                    break;
                }
                case "ciManagement": {
                    this.ciManagement = new CiManagement(xml);
                    break;
                }
                case "inceptionYear": {
                    this.inceptionYear = xml.getElementText();
                    break;
                }
                case "mailingLists": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("mailingList")) {
                            this.mailingLists.add(new MailingList(xml));
                        } else {
                            throw new XMLStreamException("Expected <mailingList> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "developers": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("developer")) {
                            this.developers.add(new Developer(xml));
                        } else {
                            throw new XMLStreamException("Expected <developer> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "contributors": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("contributor")) {
                            this.contributors.add(new Contributor(xml));
                        } else {
                            throw new XMLStreamException("Expected <contributor> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "licenses": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("license")) {
                            this.licenses.add(new License(xml));
                        } else {
                            throw new XMLStreamException("Expected <license> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "scm": {
                    this.scm = new Scm(xml);
                    break;
                }
                case "organization": {
                    this.organization = new Organization(xml);
                    break;
                }
                case "build": {
                    this.build = new Build(xml);
                    break;
                }
                case "profiles": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("profile")) {
                            this.profiles.add(new Profile(xml));
                        } else {
                            throw new XMLStreamException("Expected <profile> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "modules": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("module")) {
                            this.modules.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <module> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "repositories": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("repository")) {
                            this.repositories.add(new Repository(xml));
                        } else {
                            throw new XMLStreamException("Expected <repository> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "pluginRepositories": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("pluginRepository")) {
                            this.pluginRepositories.add(new Repository(xml));
                        } else {
                            throw new XMLStreamException("Expected <pluginRepository> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "dependencies": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("dependency")) {
                            this.dependencies.add(new Dependency(xml));
                        } else {
                            throw new XMLStreamException("Expected <dependency> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "reports": {
                    for (int depth = 1; depth > 0;) {
                        if (xml.next() == START_ELEMENT) {
                            depth++;
                        } else if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                            depth--;
                        }
                    }
                    break;
                }
                case "reporting": {
                    this.reporting = new Reporting(xml);
                    break;
                }
                case "dependencyManagement": {
                    this.dependencyManagement = new DependencyManagement(xml);
                    break;
                }
                case "distributionManagement": {
                    this.distributionManagement = new DistributionManagement(xml);
                    break;
                }
                case "properties": {
                    while (xml.nextTag() == START_ELEMENT) {
                        this.properties.put(xml.getLocalName(), xml.getElementText());
                    }
                    break;
                }
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public Parent getParent() {
        return parent;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public Prerequisites getPrerequisites() {
        return prerequisites;
    }

    public IssueManagement getIssueManagement() {
        return issueManagement;
    }

    public CiManagement getCiManagement() {
        return ciManagement;
    }

    public String getInceptionYear() {
        return inceptionYear;
    }

    public List<MailingList> getMailingLists() {
        return mailingLists;
    }

    public List<Developer> getDevelopers() {
        return developers;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public Scm getScm() {
        return scm;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Build getBuild() {
        return build;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public List<String> getModules() {
        return modules;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public List<Repository> getPluginRepositories() {
        return pluginRepositories;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public Map<String,String> getReports() {
        return reports;
    }

    public Reporting getReporting() {
        return reporting;
    }

    public DependencyManagement getDependencyManagement() {
        return dependencyManagement;
    }

    public DistributionManagement getDistributionManagement() {
        return distributionManagement;
    }

    public Map<String,String> getProperties() {
        return properties;
    }
}

