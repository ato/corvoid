package corvoid;

import java.util.Collections;
import java.util.Set;

class Dependency {
	final VersionedCoord coord = new VersionedCoord();
	String scope = "compile";
	Set<Coord> exclusions = Collections.emptySet();
	boolean optional = false;
}