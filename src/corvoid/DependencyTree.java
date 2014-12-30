package corvoid;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.stream.XMLStreamException;

import corvoid.pom.Dependency;
import corvoid.pom.Exclusion;
import corvoid.pom.Model;

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
		
		public Model getModel() {
			return model;
		}
		
		public File file() {
			return cache.artifactPath(new Coord(model.getGroupId(), model.getArtifactId()), model.getVersion(), source.getType());
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
						&& (dep.getOptional() == null || dep.getOptional() == false)) {
					String version = dep.getVersion();
					if (version == null) {
						// try to find version in DependencyManagement section
						for (Dependency dm : model.getDependencyManagement().getDependencies()) {
							if (dm.getArtifactId().equals(dep.getArtifactId()) && dm.getGroupId().equals(dep.getGroupId())) {
								version = dm.getVersion();
							}
						}
					}
					if (version == null) {
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
		
		void print(PrintStream out) {
			for (int i = 0; i < depth; i++) {
				out.print("  ");
			}
			out.format("%s:%s\n", model.getGroupId(), model.getArtifactId());
		}
		
		public List<Node> children() {
			return children;
		}
	}

	public void resolve(Model project) throws XMLStreamException, IOException {
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
			Coord coord = new Coord(node.source.getGroupId(), node.source.getArtifactId());
			out.add(cache.artifactPath(coord, versions.get(coord), node.source.getType()));
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
	
	private void print(Node node, PrintStream out) {
		node.print(out);
		for (Node child: node.children) {
			print(child, out);
		}
	}
	
	public void print(PrintStream out) {
		print(root, out);
		out.println("\nUnconstrained:");
		for (Coord coord : unconstrained) {
			out.println(coord);
		}

	}

	public void fetchDependencies(Node node) throws IOException {
		if (node.source != null) {
			Coord coord = new Coord(node.source.getGroupId(), node.source.getArtifactId());
			cache.fetch(coord, versions.get(coord), node.source.getType());			
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