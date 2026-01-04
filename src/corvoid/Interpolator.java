package corvoid;

import corvoid.pom.Model;
import corvoid.pom.Transformer;

class Interpolator implements Transformer {
	private final Model project;

	private Interpolator(Model project) {
		this.project = project;
	}

	static void interpolate(Model project) {
		project.transform(new Interpolator(project));
	}

	private String interpolate(String s) {
		if (s == null) return s;
		int pos = s.indexOf('$');
		if (pos < 0) return s;
		StringBuilder out = null;
		for (;;) {
			int i = s.indexOf("${", pos);
			if (i < 0) {
				break;
			}
			int j = s.indexOf('}', i + 2);
			if (j < 0) {
				break;
			}
			String key = s.substring(i + 2, j);
			if (out == null) out = new StringBuilder(s.length());
			out.append(s, pos, i);
			out.append(interpolate(resolveInterpolation(key)));
			pos = j + 1;
		}
		if (out == null) return s;
		out.append(s.substring(pos));
		return out.toString();
	}

	private String resolveInterpolation(String key) {
		switch (key) {
		case "basedir":
		case "project.basedir":
			return "./";
		case "project.groupId":
		case "pom.groupId":
			return project.getGroupId();
		case "project.artifactId":
		case "pom.artifactId":
			return project.getArtifactId();
		case "project.version":
		case "pom.version":
			return project.getVersion();
		case "project.build.directory":
			return project.getBuild().getDirectory();
		}
		String value = project.getProperties().get(key);
		if (value != null) {
			return value;
		}
		if (System.getenv("CORVOID_VERBOSE") != null) {
			System.err.println(project.getGroupId() + ":" + project.getArtifactId()
					+ ":" + project.getVersion()
					+ " Warning: unimplemented interpolation: " + key);
		}
		return key;
	}

	@Override
	public String transform(String s) {
		return interpolate(s);
	}
}