package corvoid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Interpolator {
	private Project project;
	
	private Interpolator(Project project) {
		this.project = project;
	}

	static void interpolate(Project project) {
		new Interpolator(project).interpolateProject();
	}
	
	private void interpolateProject() {
		project.groupId = interpolate(project.groupId);
		project.artifactId = interpolate(project.artifactId);
		project.version = interpolate(project.version);
		project.classifier = interpolate(project.classifier);
		project.name = interpolate(project.name);
		project.packaging = interpolate(project.packaging);	
		for (Dependency dep : project.dependencies) {
			interpolateDependency(dep);
		}
		for (Dependency dep : project.dependencyManagement.values()) {
			interpolateDependency(dep);
		}
	}
	
	private void interpolateDependency(Dependency dep) {
		dep.coord.groupId = interpolate(dep.coord.groupId);
		dep.coord.artifactId = interpolate(dep.coord.artifactId);
		dep.coord.version = interpolate(dep.coord.version);
		dep.coord.type = interpolate(dep.coord.type);
		dep.scope = interpolate(dep.scope);
	}
	private static final Pattern RE_INTERP = Pattern.compile("(^|[^\\\\])\\$\\{([^}]+)\\}");

	private String interpolate(String s) {
		if (s == null) return s;
		StringBuilder out = new StringBuilder();
		Matcher m = RE_INTERP.matcher(s);
		int last = 0;
		while (m.find()) {
			if (m.end(1) > last) {
				out.append(s.substring(last, m.end(1)));
				last = m.end(1);
			}
			String key = m.group(2);
			out.append(resolveInterpolation(key));
			last = m.end();
		}
		if (last == 0) {
			return s;
		}
		out.append(s.substring(last, s.length()));
		return out.toString();
	}
	
	private String resolveInterpolation(String key) {
		switch (key) {
		case "project.groupId":    return project.groupId;
		case "project.artifactId": return project.artifactId;
		case "project.version":    return project.version;
		case "pom.groupId":    return project.groupId;
		case "pom.artifactId": return project.artifactId;
		case "pom.version":    return project.version;
		}
		String value = project.properties.get(key);
		if (value != null) {
			return value;
		}
		throw new IllegalArgumentException("unimplemented interpolation: " + key);
	}
}