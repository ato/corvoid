package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class Resource {
    private String targetPath;
    private boolean filtering = false;
    private String directory;
    private List<String> includes = new ArrayList<>();
    private List<String> excludes = new ArrayList<>();

    public Resource() {}

    public Resource(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
                case "targetPath": {
                    this.targetPath = xml.getElementText();
                    break;
                }
                case "filtering": {
                    this.filtering = Boolean.parseBoolean(xml.getElementText());
                    break;
                }
                case "directory": {
                    this.directory = xml.getElementText();
                    break;
                }
                case "includes": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("include")) {
                            this.includes.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <include> but got: " + xml.getLocalName(), xml.getLocation());
                        }
                    }
                    break;
                }
                case "excludes": {
                    while (xml.nextTag() == START_ELEMENT) {
                        if (xml.getLocalName().equals("exclude")) {
                            this.excludes.add(xml.getElementText());
                        } else {
                            throw new XMLStreamException("Expected <exclude> but got: " + xml.getLocalName(), xml.getLocation());
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

    public String getTargetPath() {
        return targetPath;
    }

    public boolean isFiltering() {
        return filtering;
    }

    public String getDirectory() {
        return directory;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }
}

