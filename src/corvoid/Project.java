package corvoid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Project {
	String groupId, artifactId, version, classifier, packaging = "jar";
	String name;
	VersionedCoord parent = null;
	List<Dependency> dependencies = new ArrayList<>();
	Map<String, String> properties = new HashMap<>();
	Map<Coord,Dependency> dependencyManagement = new HashMap<>();
	
	void inherit(Project parent) {
		if (groupId == null) {
			groupId = parent.groupId;
		}
		if (version == null) {
			version = parent.version;
		}
		Maps.putAllIfAbsent(properties, parent.properties);
		Maps.putAllIfAbsent(dependencyManagement, parent.dependencyManagement);
		dependencies.addAll(parent.dependencies);
	}
	
	public String toString() {
		return groupId + ":" + artifactId + ":" + version + ":" + packaging;
	}
}