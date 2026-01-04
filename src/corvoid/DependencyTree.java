package corvoid;

import corvoid.pom.Dependency;
import corvoid.pom.Exclusion;
import corvoid.pom.License;
import corvoid.pom.Model;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DependencyTree {
	Workspace workspace;
	Map<Coord,String> versions = new ConcurrentHashMap<>();
	Set<Coord> unconstrained = Collections.newSetFromMap(new ConcurrentHashMap<>());
	Node root;

	public DependencyTree(Workspace workspace) {
		this.workspace = workspace;
	}
	
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
		
		public Path file() {
			return workspace.artifactPath(new Coord(model.getGroupId(), model.getArtifactId()), model.getVersion(), source.getClassifier(), source.getType());
		}
		
		public String getArtifactId() {
			return model.getArtifactId();
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
				Path path = artifactPath();
				if (path != null && Files.exists(path)) {
					try {
						total += Files.size(path);
					} catch (IOException e) {
						// ignore
					}
				}
				for (Node child : children) {
					total += child.totalSize();
				}
				totalSize = total;
			}
			return totalSize;
		}
		
		void print(PrintStream out, String prefix, boolean isLast, long rootTotal, boolean sort, boolean showGroupId) {
			String cs;
			String currentPrefix = "";
			String nextPrefix = "";
			if (depth > 0) {
				currentPrefix = "\033[90m" + prefix + (isLast ? "└── " : "├── ") + "\033[0m";
				nextPrefix = prefix + (isLast ? "    " : "│   ");
			}
			
			if (model.getArtifactId().equals(model.getGroupId()) || !showGroupId) {
				cs = String.format("%s\033[1;36m%s\033[0m \033[1;33m%s\033[0m", currentPrefix, model.getArtifactId(), version());
			} else {
				cs = String.format("%s\033[90m%s:\033[1;36m%s\033[0m \033[1;33m%s\033[0m", currentPrefix, model.getGroupId(), model.getArtifactId(), version());
			}
			long nodeTotal = totalSize();
			String totalSizeStr = formatBytes(nodeTotal);
			if (children.isEmpty() || nodeTotal == 0) totalSizeStr = "";
			double percentValue = 100.0 * nodeTotal / rootTotal;
			String percent = rootTotal > 0 ? String.format(percentValue < 10.0 ? "%.1f%%" : "%.0f%%", percentValue) : (rootTotal == 0 && nodeTotal == 0 && depth == 0 ? "100.0%" : "");

			Path path = artifactPath();
			String license = license();

			String format = "%-60s %8s %8s %6s   %s\n";
			int ansiLength = cs.length() - cs.replaceAll("\033\\[[0-9;]*m", "").length();
			String padding = " ".repeat(Math.max(0, 60 + ansiLength - cs.length()));
			String paddedCs = cs + padding;

			if (path != null && Files.exists(path)) {
				try {
					out.format("%s %8s %8s %6s   %s\n", paddedCs, formatBytes(Files.size(path)), totalSizeStr, percent, license);
				} catch (IOException e) {
					out.format("%s %8s %8s %6s   %s\n", paddedCs, "", totalSizeStr, percent, license);
				}
			} else {
				out.format("%s %8s %8s %6s   %s\n", paddedCs, "", totalSizeStr, percent, license);
			}

			if (sort) children.sort(Comparator.comparing(Node::totalSize).reversed());

			for (int i = 0; i < children.size(); i++) {
				children.get(i).print(out, nextPrefix, i == children.size() - 1, rootTotal, sort, showGroupId);
			}
		}

        private String license() {
			if (model.getLicenses() == null || model.getLicenses().isEmpty()) return "";
			return model.getLicenses().stream().map(License::getName)
					.filter(Objects::nonNull)
					.map(Node::normalizeLicenseName)
					.distinct()
					.sorted()
					.collect(Collectors.joining(", "));
		}

		private static String normalizeLicenseName(String name) {
			return switch (name) {
				case "Apache License, Version 2.0", "The Apache Software License, Version 2.0",
					 "The Apache License, Version 2.0", "Apache 2.0 license", "Apache License 2.0",
					 "Apache-2.0" -> "Apache 2.0";
				case "The MIT License", "MIT License" -> "MIT";
				case "BSD License" -> "BSD";
				case "GNU General Public License, version 2 with the GNU Classpath Exception", "GPL2 w/ CPE" -> "GPLv2 w/ CPE";

				case "GNU General Public License v3.0" -> "GPLv3";
				case "GNU Lesser General Public License" -> "LGPL";
				case "GNU Lesser General Public License version 2.1 or later" -> "LGPLv2.1+";
				case "Mozilla Public License 1.1 (MPL 1.1)" -> "MPL 1.1";
				case "GNU Lesser General Public License (LGPL)" -> "LGPL";
				case "Eclipse Public License v. 2.0" -> "EPL 2.0";
				default -> name;
			};
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

		Path artifactPath() {
			if (source == null) {
				return null;
			}
			return workspace.artifactPath(coord(), version(), source.getClassifier(), source.getType());
		}

		public List<Node> children() {
			return children;
		}
	}

	public void resolve(Model project) throws XMLStreamException, IOException {
		workspace.resolveImports(project);
		root = new Node();
		root.depth = 0;
		root.exclusions = new HashSet<>();
		root.model = project;

		Queue<Node> queue = new LinkedList<>();
		queue.add(root);

		while (!queue.isEmpty()) {
			int levelSize = queue.size();
			List<Node> currentLevelNodes = new ArrayList<>();

			for (int i = 0; i < levelSize; i++) {
				Node parent = queue.poll();
				parent.children = new ArrayList<>();
				for (Dependency dep : parent.model.getDependencies()) {
					Coord coord = new Coord(dep.getGroupId(), dep.getArtifactId());
					if (!parent.exclusions.contains(coord) &&
							(dep.getScope() == null || dep.getScope().equals("compile"))
							&& (dep.getOptional() == null || !dep.getOptional())) {

						String version = root.model.findManagedVersion(dep);
						if (version == null) {
							version = dep.getVersion();
						}
						if (version == null) {
							version = parent.model.findManagedVersion(dep);
						}

						if (version == null || version.startsWith("[") || version.startsWith("(")) {
							if (!versions.containsKey(coord)) {
								unconstrained.add(coord);
							}
						} else {
							if (versions.putIfAbsent(coord, version) == null) {
								unconstrained.remove(coord);
								Node node = new Node();
								node.depth = parent.depth + 1;
								node.exclusions = new HashSet<>(parent.exclusions);
								for (Exclusion exclusion : dep.getExclusions()) {
									node.exclusions.add(new Coord(exclusion.getGroupId(), exclusion.getArtifactId()));
								}
								node.source = dep;
								String finalVersion = version;
								Node finalNode = node;
								node.future = workspace.executor.submit(() -> {
									Model m = workspace.resolveProject(coord, finalVersion);
									finalNode.model = m;
									return m;
								});
								parent.children.add(node);
								currentLevelNodes.add(node);
							}
						}
					}
				}
			}

			for (Node node : currentLevelNodes) {
				try {
					node.future.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
			queue.addAll(currentLevelNodes);
		}
	}
	
	private void buildClasspath(Node node, List<Path> out, Set<Coord> seen) {
		if (node.source != null) {
			Coord coord = node.coord();
			if (seen.add(coord)) {
				out.add(node.artifactPath());
			}
		}
		for (Node child : node.children) {
			buildClasspath(child, out, seen);
		}
	}

	public List<Path> classpathFiles() {
		List<Path> files = new ArrayList<>();
		Set<Coord> seen = new HashSet<>();
		buildClasspath(root, files, seen);
		return files;
	}

	public List<String> classpathStrings() {
		List<Path> files = classpathFiles();
		List<String> strings = new ArrayList<>(files.size());
		for (Path file : files) {
			strings.add(file.toString());
		}
		return strings;
	}

	public String classpath() {
		return String.join(":", classpathStrings());
	}
	
	public void print(PrintStream out, boolean sort, boolean showGroupId) {
		out.format("\033[90m%-60s %8s %8s %6s   %s\033[0m\n", "Artifact", "Size", "Total", "%", "License");
		if (root != null) {
			root.print(out, "", true, root.totalSize(), sort, showGroupId);
		}
		if (!unconstrained.isEmpty()) {
			out.println("\n\033[1;31mUnconstrained:\033[0m");
			for (Coord coord : unconstrained) {
				out.println(coord);
			}
		}
	}

	public void fetchDependencies(Node node) throws IOException {
		List<Future<?>> futures = new ArrayList<>();
		fetchDependenciesRecursive(node, futures);
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new IOException(e);
			}
		}
	}

	private void fetchDependenciesRecursive(Node node, List<Future<?>> futures) {
		if (node.source != null && !workspace.isLocalModule(node.coord())) {
			Coord coord = node.coord();
			futures.add(workspace.executor.submit(() -> {
				workspace.getCache().fetch(coord, versions.get(coord), node.source.getClassifier(), node.source.getType());
				return null;
			}));
		}
		for (Node child : node.children) {
			fetchDependenciesRecursive(child, futures);
		}
	}
	
	public void fetchDependencies() throws IOException {
		fetchDependencies(root);
	}
	
	public Node root() {
		return root;
	}
	
}