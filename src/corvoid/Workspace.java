package corvoid;

import corvoid.pom.Model;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Workspace {
	private final Map<Coord, Path> localModules = new HashMap<>();
	private final Cache cache;

	public Workspace(Cache cache) {
		this.cache = cache;
	}

	public void addLocalModule(Coord coord, Path pomPath) {
		localModules.put(coord, pomPath);
	}

	public boolean isLocalModule(Coord coord) {
		return localModules.containsKey(coord);
	}

	public Path getLocalModulePom(Coord coord) {
		return localModules.get(coord);
	}

	public Model resolveProject(Coord coord, String version) throws XMLStreamException, IOException {
		Path localPom = localModules.get(coord);
		Model output;
		if (localPom != null) {
			output = Corvoid.parseModel(localPom);
			if (output.getParent() != null && output.getParent().getArtifactId() != null) {
				String relativePath = output.getParent().getRelativePath();
				if (relativePath == null) relativePath = "../pom.xml";
				Path parentPom = localPom.getParent().resolve(relativePath).normalize();
				output = new Model(Corvoid.parseModel(parentPom), output);
			}
		} else {
			output = cache.readProject(coord, version);
			Model project = output;
			while (project.getParent() != null && project.getParent().getArtifactId() != null) {
				project = cache.readProject(new Coord(project.getParent().getGroupId(), project.getParent().getArtifactId()), project.getParent().getVersion());
				output = new Model(project, output);
			}
		}
		Interpolator.interpolate(output);
		resolveImports(output);
		return output;
	}

	public void resolveImports(Model output) throws XMLStreamException, IOException {
		var dependencies = output.getDependencyManagement().getDependencies();
		for (int i = 0; i < dependencies.size(); i++) {
			var dep = dependencies.get(i);
			if ("import".equals(dep.getScope()) && "pom".equals(dep.getType())) {
				Model imported = resolveProject(new Coord(dep.getGroupId(), dep.getArtifactId()), dep.getVersion());
				output.getDependencyManagement().getDependencies().addAll(i, imported.getDependencyManagement().getDependencies());
				i += imported.getDependencyManagement().getDependencies().size();
				output.getDependencyManagement().getDependencies().remove(i);
				i--;
			}
		}
	}

	public Path artifactPath(Coord coord, String version, String classifier, String type) {
		Path localPom = localModules.get(coord);
		if (localPom != null) {
			return localPom.getParent().resolve("target").resolve("classes");
		}
		return cache.artifactPath(coord, version, classifier, type);
	}

	public Cache getCache() {
		return cache;
	}
}
