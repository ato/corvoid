package corvoid;

import java.util.Objects;

class Coord {
	final String groupId, artifactId;

	public Coord(String groupId, String artifactId) {
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, artifactId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Coord other = (Coord) obj;
		return Objects.equals(groupId, other.groupId) &&
				Objects.equals(artifactId, other.artifactId);
	}
	
	public String toString() {
		return groupId + ":" + artifactId;
	}
}