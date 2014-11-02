package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class ReportPlugin {
    private String groupId;
    private String artifactId;
    private String version;
    private String inherited;
    private Map<String,String> configuration = new HashMap<>();
    private List<ReportSet> reportSets = new ArrayList<>();

    public ReportPlugin() {}

    public ReportPlugin(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "groupId": {
                    this.groupId = xml.getElementText();
                    break;
                }
                case "artifactId": {
                    this.artifactId = xml.getElementText();
                    break;
                }
                case "version": {
                    this.version = xml.getElementText();
                    break;
                }
                case "inherited": {
                    this.inherited = xml.getElementText();
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
                case "reportSets": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("reportSet")) {
                            this.reportSets.add(new ReportSet(xml));
                        } else {
                            throw new XMLStreamException("Expected <reportSet> but got: " + xml.getLocalName(), xml.getLocation());
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

    public ReportPlugin(ReportPlugin reportPlugin1, ReportPlugin reportPlugin2) {
        groupId = reportPlugin2.groupId == null ? reportPlugin1.groupId : reportPlugin2.groupId;
        artifactId = reportPlugin2.artifactId == null ? reportPlugin1.artifactId : reportPlugin2.artifactId;
        version = reportPlugin2.version == null ? reportPlugin1.version : reportPlugin2.version;
        inherited = reportPlugin2.inherited == null ? reportPlugin1.inherited : reportPlugin2.inherited;
        configuration.putAll(reportPlugin1.configuration);
        configuration.putAll(reportPlugin2.configuration);
        reportSets.addAll(reportPlugin1.reportSets);
        reportSets.addAll(reportPlugin2.reportSets);
    }

    public void transform(Transformer transformer) {
        groupId = transformer.transform(groupId);
        artifactId = transformer.transform(artifactId);
        version = transformer.transform(version);
        inherited = transformer.transform(inherited);
        for (String key: configuration.keySet()) {
            configuration.put(key, transformer.transform(configuration.get(key)));
        }
        for (int i = 0; i < reportSets.size(); i++) {
            reportSets.get(i).transform(transformer);
        }
    }


    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getInherited() {
        return inherited;
    }

    public Map<String,String> getConfiguration() {
        return configuration;
    }

    public List<ReportSet> getReportSets() {
        return reportSets;
    }
}

