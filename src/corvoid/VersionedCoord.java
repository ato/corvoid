package corvoid;

import java.util.regex.Pattern;

class VersionedCoord {
	String groupId, artifactId, version, type = "jar";
	
	public String toString() {
		return groupId + ":" + artifactId + ":" + version + ":" + type; 
	}
	
	Coord unversioned() {
		return new Coord(groupId, artifactId);
	}
	
	private static final Pattern RE_ID = Pattern.compile("[A-Za-z0-9_\\-.]+");
	
	private static String validateId(String id) {
		if (RE_ID.matcher(id).matches() && !id.equals(".") && !id.equals("..")) {
			return id;
		}
		throw new IllegalArgumentException("Invalid id: " + id);
	}
	
	VersionedCoord validate() {
		validateId(groupId);
		validateId(artifactId);
		validateId(version);
		validateId(type);
		return this;
	}
}