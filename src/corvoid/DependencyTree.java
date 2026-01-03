package corvoid;

import corvoid.pom.Dependency;
import corvoid.pom.Exclusion;
import corvoid.pom.Model;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DependencyTree {
	Cache cache = new Cache();
	Map<Coord,String> versions = new HashMap<>();
	Set<Coord> unconstrained = new HashSet<>();
	Node root;
	
	public class Node {
		Set<Coord> exclusions;
		int depth;
		Model model;
		Future<Model> future;
		Dependency source;
		List<Node> children;
		private long totalSize = -1;
		
		public Model getModel() {
			return model;
		}
		
		public File file() {
			return cache.artifactPath(new Coord(model.getGroupId(), model.getArtifactId()), model.getVersion(), source.getClassifier(), source.getType());
		}
		
		public String getArtifactId() {
			return model.getArtifactId();
		}
		
		void resolve() throws XMLStreamException, IOException {
			children = new ArrayList<>();
			for (Dependency dep : model.getDependencies()) {
				Coord coord = new Coord(dep.getGroupId(), dep.getArtifactId());
				if (!exclusions.contains(coord) && 
						!versions.containsKey(coord) && 
						(dep.getScope() == null || dep.getScope().equals("compile")) 
						&& (dep.getOptional() == null || !dep.getOptional())) {
					String version = dep.getVersion();
					if (version == null) {
						// try to find version in DependencyManagement section
						for (Dependency dm : model.getDependencyManagement().getDependencies()) {
							if (dm.getArtifactId().equals(dep.getArtifactId()) && dm.getGroupId().equals(dep.getGroupId())) {
								version = dm.getVersion();
							}
						}
					}
					if (version == null || version.startsWith("[") || version.startsWith("(")) {
						unconstrained.add(coord);
					} else {
						unconstrained.remove(coord);
						versions.put(coord, version);
						Node node = new Node();
						node.depth = depth + 1;
						node.exclusions = new HashSet<>(exclusions);
						for (Exclusion exclusion : dep.getExclusions()) {
							node.exclusions.add(new Coord(exclusion.getGroupId(), exclusion.getArtifactId()));
						}
						node.model = cache.readAndInheritProject(coord, version);
						node.source = dep;
						children.add(node);
					}
				}
			}
			for (Node node : children) {
				try {
					if (node.future != null)
						node.model = node.future.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}

				try {
					node.resolve();
				} catch (Throwable t) {
					System.err.println("XX " + node.model +  " via " + model);
					throw t;
				}
			}
		}

		/* based on aioobe's http://stackoverflow.com/a/3758880 */
		public String formatBytes(long bytes) {
			int unit = 1024;
			if (bytes < unit) return String.format("%3d   B", bytes);
			int exp = (int) (Math.log(bytes) / Math.log(unit));
			char pre = "KMGTPE".charAt(exp - 1);
			double val = bytes / Math.pow(unit, exp);
			if (val < 10) {
				return String.format("%3.1f %cB", val, pre);
			} else {
				return String.format("%3.0f %cB", val, pre);
			}
		}

		long totalSize() {
			if (totalSize < 0) {
				long total = 0;
				File path = artifactPath();
				if (path != null && path.exists()) {
					total += path.length();
				}
				for (Node child : children) {
					total += child.totalSize();
				}
				totalSize = total;
			}
			return totalSize;
		}
		
		void print(PrintStream out, String prefix, boolean isLast, long rootTotal, boolean sort) {
			String cs;
			String currentPrefix = "";
			String nextPrefix = "";
			if (depth > 0) {
				currentPrefix = prefix + (isLast ? "└── " : "├── ");
				nextPrefix = prefix + (isLast ? "    " : "│   ");
			}
			
			if (model.getArtifactId().equals(model.getGroupId())) {
				cs = String.format("%s%s %s", currentPrefix, model.getArtifactId(), version());
			} else {
				cs = String.format("%s%s:%s %s", currentPrefix, model.getGroupId(), model.getArtifactId(), version());
			}
			long nodeTotal = totalSize();
			String totalSizeStr = formatBytes(nodeTotal);
			double percentValue = 100.0 * nodeTotal / rootTotal;
			String percent = rootTotal > 0 ? String.format(percentValue < 10.0 ? "%.1f%%" : "%.0f%%", percentValue) : (rootTotal == 0 && nodeTotal == 0 && depth == 0 ? "100.0%" : "");

			File path = artifactPath();
			if (path != null && path.exists()) {
				out.format("%-60s %8s %8s %6s\n", cs, formatBytes(path.length()), totalSizeStr, percent);
			} else {
				out.format("%-60s %8s %8s %6s\n", cs, "", totalSizeStr, percent);
			}

			if (sort) children.sort(Comparator.comparing(Node::totalSize).reversed());

			for (int i = 0; i < children.size(); i++) {
				children.get(i).print(out, nextPrefix, i == children.size() - 1, rootTotal, sort);
			}
		}

		Coord coord() {
			return new Coord(model.getGroupId(), model.getArtifactId());
		}

		String version() {
			if (model == null) return null;
			String v = versions.get(coord());
			if (v == null) return model.getVersion();
			return v;
		}

		File artifactPath() {
			if (source == null) {
				return null;
			}
			return cache.artifactPath(coord(), version(), source.getClassifier(), source.getType());
		}

		public List<Node> children() {
			return children;
		}
	}

	public void resolve(Model project) throws XMLStreamException, IOException {
		cache.resolveImports(project);
		try {
			Node node = new Node();
			node.depth = 0;
			node.exclusions = new HashSet<>();
			node.model = project;
			node.resolve();
			root = node;
		} finally {
			//cache.threadPool.shutdown();
		}
	}
	
	private void buildClasspath(Node node, List<File> out) {
		if (node.source != null) {
			out.add(node.artifactPath());
		}
		for (Node child: node.children) {
			buildClasspath(child, out);
		}
	}
	
	public List<File> classpathFiles() {
		List<File> files = new ArrayList<>();
		buildClasspath(root, files);
		return files;
	}

	public List<String> classpathStrings() {
		List<File> files = classpathFiles();
		List<String> strings = new ArrayList<>(files.size());
		for (File file : files) {
			strings.add(file.toString());
		}
		return strings;
	}

	public String classpath() {
		return String.join(":", classpathStrings());
	}
	
	public void print(PrintStream out, boolean sort) {
		out.format("%-60s %8s %8s %6s\n", "Artifact", "Size", "Total", "%");
		if (root != null) {
			root.print(out, "", true, root.totalSize(), sort);
		}
		if (!unconstrained.isEmpty()) {
			out.println("\nUnconstrained:");
			for (Coord coord : unconstrained) {
				out.println(coord);
			}
		}
	}

	public void fetchDependencies(Node node) throws IOException {
		if (node.source != null) {
			Coord coord = new Coord(node.source.getGroupId(), node.source.getArtifactId());
			cache.fetch(coord, versions.get(coord), node.source.getClassifier(), node.source.getType());
		}
		for (Node child: node.children) {
			fetchDependencies(child);
		}
	}
	
	public void fetchDependencies() throws IOException {
		fetchDependencies(root);
	}
	
	public Node root() {
		return root;
	}
	
}