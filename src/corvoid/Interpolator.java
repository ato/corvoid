package corvoid;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corvoid.pom.Model;

class Interpolator {
	private Model project;

	private Interpolator(Model project) {
		this.project = project;
	}

	static void interpolate(Model project) {
		new Interpolator(project).interpolateFields(project);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void interpolateFields(Object obj) {
		Class<?> clazz = obj.getClass();
		try {
			for (Field f : clazz.getDeclaredFields()) {
				Object value;
				f.setAccessible(true);
				value = f.get(obj);
				if (value == null || value instanceof Boolean || value instanceof Number) {
					// do nothing
				} else if (value instanceof String) {
					f.set(obj, interpolate((String) value));
				} else if (value instanceof List) {
					List<Object> replacements = new ArrayList<>();
 					for (Object x : (List) value) {
 						if (x instanceof String) {
 							replacements.add(interpolate((String)x));
 						} else {
 							interpolateFields(x);
 							replacements.add(x);
 						}
					}
					((List)value).clear();
					((List)value).addAll(replacements);
				} else if (value instanceof Map) {
					Map map = (Map) value;
					for (Object key : map.keySet()) {
						Object val = map.get(key);
						if (val != null && val instanceof String) {
							map.put(key, interpolate((String)val));
						}
					}
				} else {
					interpolateFields(value);
				}
			}
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static final Pattern RE_INTERP = Pattern
			.compile("(^|[^\\\\])\\$\\{([^}]+)\\}");

	private String interpolate(String s) {
		if (s == null)
			return s;
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
		case "basedir":
			return "./";
		case "project.groupId":
			return project.getGroupId();
		case "project.artifactId":
			return project.getArtifactId();
		case "project.version":
			return project.getVersion();
		case "pom.groupId":
			return project.getGroupId();
		case "pom.artifactId":
			return project.getArtifactId();
		case "pom.version":
			return project.getVersion();
		}
		String value = project.getProperties().get(key);
		if (value != null) {
			return value;
		}
		System.err.println(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion() + " Warning: unimplemented interpolation: " + key);
		return key;
	}
}