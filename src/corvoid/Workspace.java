package corvoid;

import corvoid.pom.Model;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Workspace {
	final ExecutorService executor = Executors.newFixedThreadPool(8, Thread.ofPlatform().daemon()
			.name("Workspace-", 1).factory());
	private final Map<Coord, Path> localModules = new HashMap<>();
	private final Cache cache;
	private final Map<Path, Model> models = new ConcurrentHashMap<>();

	public Workspace(Cache cache) {
		this.cache = cache;
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
			output = Model.read(localPom);
			if (output.getParent() != null && output.getParent().getArtifactId() != null) {
				String relativePath = output.getParent().getRelativePath();
				if (relativePath == null) relativePath = "../pom.xml";
				Path parentPom = localPom.getParent().resolve(relativePath).normalize();
				output = new Model(Model.read(parentPom), output);
			}
		} else {
			output = getModel(coord, version);
			Model project = output;
			while (project.getParent() != null && project.getParent().getArtifactId() != null) {
				project = getModel(new Coord(project.getParent().getGroupId(), project.getParent().getArtifactId()), project.getParent().getVersion());
				output = new Model(project, output);
			}
		}
		Interpolator.interpolate(output);
		resolveImports(output);
		return output;
	}

	public Model getModel(Coord coord, String version) throws IOException {
		Path path = cache.fetch(coord, version, null, "pom");
		return models.computeIfAbsent(path, p -> {
            try {
                return Model.read(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        });
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

	public void scanModules(Path root) throws XMLStreamException, IOException {
		Path pom = root.resolve("pom.xml");
		if (Files.exists(pom)) {
			Model model = Model.read(pom);
			String groupId = model.getGroupId();
			if (groupId == null && model.getParent() != null) {
				groupId = model.getParent().getGroupId();
			}
			String artifactId = model.getArtifactId();
			if (groupId != null && artifactId != null) {
				Coord coord = new Coord(groupId, artifactId);
				localModules.put(coord, pom);
			}
			for (String module : model.getModules()) {
				scanModules(root.resolve(module));
			}
		}
	}

	public List<Corvoid> sortedModules(Model model, Path projectRoot) throws XMLStreamException, IOException {
		List<String> modules = model.getModules();
		if (modules.isEmpty()) {
			return List.of();
		}
		scanModules(projectRoot);
		Map<Coord, Corvoid> coordToCorvoid = new LinkedHashMap<>();
		Map<Coord, Set<Coord>> dependencies = new HashMap<>();

		for (String module : modules) {
			Path modulePath = projectRoot.resolve(module);
			Corvoid cv = new Corvoid(modulePath);
			Model m = cv.parseModel();
			String groupId = m.getGroupId();
			if (groupId == null && m.getParent() != null) {
				groupId = m.getParent().getGroupId();
			}
			if (groupId != null && m.getArtifactId() != null) {
				Coord coord = new Coord(groupId, m.getArtifactId());
				coordToCorvoid.put(coord, cv);
				Set<Coord> deps = new HashSet<>();
				for (corvoid.pom.Dependency d : m.getDependencies()) {
					String dGroupId = d.getGroupId();
					if (dGroupId == null && m.getParent() != null) {
						dGroupId = m.getParent().getGroupId();
					}
					if (dGroupId != null && d.getArtifactId() != null) {
						Coord dCoord = new Coord(dGroupId, d.getArtifactId());
						if (isLocalModule(dCoord)) {
							deps.add(dCoord);
						}
					}
				}
				dependencies.put(coord, deps);
			}
		}

		List<Corvoid> sorted = new ArrayList<>();
		Set<Coord> visited = new HashSet<>();
		Set<Coord> visiting = new HashSet<>();

		for (Coord coord : coordToCorvoid.keySet()) {
			visit(coord, dependencies, visited, visiting, sorted, coordToCorvoid);
		}
		return sorted;
	}

	private void visit(Coord coord, Map<Coord, Set<Coord>> dependencies, Set<Coord> visited, Set<Coord> visiting,
					   List<Corvoid> sorted, Map<Coord, Corvoid> coordToCorvoid) {
		if (visited.contains(coord)) return;
		if (visiting.contains(coord)) {
			throw new RuntimeException("Circular dependency detected involving " + coord);
		}
		visiting.add(coord);
		Set<Coord> deps = dependencies.get(coord);
		if (deps != null) {
			for (Coord dep : deps) {
				if (coordToCorvoid.containsKey(dep)) {
					visit(dep, dependencies, visited, visiting, sorted, coordToCorvoid);
				}
			}
		}
		visiting.remove(coord);
		visited.add(coord);
		sorted.add(coordToCorvoid.get(coord));
	}
}
