package corvoid.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamReader.START_ELEMENT;

public class DependencyManagement {
    private List<Dependency> dependencies = new ArrayList<>();

    public DependencyManagement() {}

    public DependencyManagement(XMLStreamReader xml) throws XMLStreamException {
        while (xml.nextTag() == START_ELEMENT) {
            switch(xml.getLocalName()) {
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
                default: {
                    throw new XMLStreamException("Unexpected tag: " + xml.getLocalName(), xml.getLocation());
                }
            }
        }
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}

