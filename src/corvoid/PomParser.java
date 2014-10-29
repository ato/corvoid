package corvoid;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class PomParser {
	final Project project;
	final XMLStreamReader rdr;
	
	private PomParser(InputStream in) throws XMLStreamException, FactoryConfigurationError {
		rdr = XMLInputFactory.newInstance().createXMLStreamReader(in);
		project = new Project();			
		rdr.nextTag();
		if (rdr.getLocalName().equals("project")) {
			try {
				parseProject();
			} catch (RuntimeException e) {
				System.err.println(rdr.getLocation());
				throw e;
			}
		}
	}
	
	static Project parse(InputStream in) throws XMLStreamException {
		PomParser parser = new PomParser(in);			
		return parser.project;
	}
	
	private void skipToEndOfTag() throws XMLStreamException {
		int level = 1;
		while (rdr.hasNext() && level > 0) {
			int elType = rdr.next();
			if (elType == START_ELEMENT) {
				level++;
			} else if (elType == END_ELEMENT) {
				level--;
			}
		}
	}

	private void parseProject() throws XMLStreamException {
		while (rdr.hasNext() && rdr.nextTag() == START_ELEMENT) {
			switch (rdr.getLocalName()) {
			case "parent":       parseParent(); break;
			case "name":         project.name = rdr.getElementText(); break;
			case "groupId":      project.groupId = rdr.getElementText(); break;
			case "artifactId":   project.artifactId = rdr.getElementText(); break;
			case "version":      project.version = rdr.getElementText(); break;
			case "packaging":    project.packaging = rdr.getElementText(); break;
			case "dependencies": project.dependencies = parseDependencies(); break;
			case "dependencyManagement": project.dependencyManagement = parseDependencyManagement(); break;
			case "properties":   parseProperties(); break;
			default: skipToEndOfTag(); break;
			}
		}
	}

	private Map<Coord, Dependency> parseDependencyManagement() throws XMLStreamException {
		HashMap<Coord, Dependency> map = new HashMap<>();
		while (rdr.hasNext() && rdr.nextTag() == START_ELEMENT) {
			switch (rdr.getLocalName()) {
			case "dependencies":
				for (Dependency dep : parseDependencies()) {
					map.put(dep.coord.unversioned(), dep);
				}
			break;
			default: skipToEndOfTag(); break;
			}
		}
		return map;
	}

	private void parseParent() throws XMLStreamException {
		VersionedCoord parent = new VersionedCoord();
		while (rdr.hasNext() && rdr.nextTag() == START_ELEMENT) {
			switch (rdr.getLocalName()) {
			case "groupId": parent.groupId = rdr.getElementText(); break;
			case "artifactId": parent.artifactId = rdr.getElementText(); break;
			case "version": parent.version = rdr.getElementText(); break;
			default: skipToEndOfTag(); break;
			}
		}
		project.parent = parent;
		if (project.groupId == null) {
			project.groupId = parent.groupId;
		}
		if (project.version == null) {
			project.version = parent.version;
		}
	}

	private void parseProperties() throws XMLStreamException {
		while (rdr.hasNext() && rdr.nextTag() == START_ELEMENT) {
			String key = rdr.getLocalName();
			String value = rdr.getElementText();
			project.properties.put(key, value);
		}
	}

	private List<Dependency> parseDependencies() throws XMLStreamException {
		List<Dependency> dependencies = new ArrayList<>();
		while (rdr.hasNext() && rdr.nextTag() == START_ELEMENT) {
			switch (rdr.getLocalName()) {
			case "dependency": dependencies.add(parseDependency()); break;
			default: skipToEndOfTag(); break;
			}
		}
		return dependencies;
	}

	private Dependency parseDependency() throws XMLStreamException {
		Dependency dep = new Dependency();
		while (rdr.hasNext() && rdr.nextTag() == START_ELEMENT) {
			switch (rdr.getLocalName()) {
			case "groupId":    dep.coord.groupId = rdr.getElementText(); break;
			case "artifactId": dep.coord.artifactId = rdr.getElementText(); break;
			case "version":    dep.coord.version = rdr.getElementText(); break;
			case "type":       dep.coord.type = rdr.getElementText(); break;
			case "scope":      dep.scope = rdr.getElementText(); break;
			case "exclusions": dep.exclusions = parseExclusions(); break;
			case "optional":   dep.optional = Boolean.valueOf(rdr.getElementText()); break;
			default: skipToEndOfTag(); break;
			}
		}
		return dep;
	}
	
	private Set<Coord> parseExclusions() throws XMLStreamException {
		Set<Coord> exclusions = new HashSet<>();
		while (rdr.hasNext() && rdr.nextTag() == START_ELEMENT) {
			switch (rdr.getLocalName()) {
			case "exclusion": exclusions.add(parseExclusion()); break;
			default: skipToEndOfTag(); break;
			}
		}
		return exclusions;
	}

	private Coord parseExclusion() throws XMLStreamException {
		String groupId = null, artifactId = null;
		while (rdr.hasNext() && rdr.nextTag() == START_ELEMENT) {
			switch (rdr.getLocalName()) {
			case "groupId":    groupId = rdr.getElementText(); break;
			case "artifactId": artifactId = rdr.getElementText(); break;
			default: skipToEndOfTag(); break;
			}
		}
		return new Coord(groupId, artifactId);
	}

}