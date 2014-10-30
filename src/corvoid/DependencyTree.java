package corvoid;

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

class DependencyTree {
	Cache cache = new Cache();
	Map<Coord,String> versions = new HashMap<>();
	Set<Coord> unconstrained = new HashSet<>();
	Node root;
	
	class Node {
		Set<Coord> exclusions;
		int depth;
		Model project;
		Future<Model> future;
		Dependency source;
		List<Node> children;
		
		void resolve() throws XMLStreamException, IOException {
			children = new ArrayList<>();
			for (Dependency dep : project.getDependencies()) {
				Coord coord = new Coord(dep.getGroupId(), dep.getArtifactId());
				if (!exclusions.contains(coord) && 
						!versions.containsKey(coord) && 
						(dep.getScope() == null || dep.getScope().equals("compile")) 
						&& !dep.isOptional()) {
					String version = dep.getVersion();
					if (version == null) {
						// try to find version in DependencyManagement section
						for (Dependency dm : project.getDependencyManagement().getDependencies()) {
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
						node.future = cache.readAsync(coord, version);
						node.source = dep;
						children.add(node);
					}
				}
			}
			for (Node node : children) {
				try {
					if (node.future != null)
						node.project = node.future.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}

				try {
					node.resolve();
				} catch (Throwable t) {
					System.err.println("XX " + node.project +  " via " + project);
					throw t;
				}
			}
		}
		
		void print(PrintStream out) {
			for (int i = 0; i < depth; i++) {
				out.print("  ");
			}
			out.format("%s:%s\n", project.getGroupId(), project.getArtifactId());
		}
	}

	public void resolve(Model project) throws XMLStreamException, IOException {
		try {
			Node node = new Node();
			node.depth = 0;
			node.exclusions = new HashSet<>();
			node.project = project;
			node.resolve();
			root = node;
		} finally {
			cache.threadPool.shutdown();
		}
	}
	
	private void buildClasspath(Node node, StringBuilder out) {
		if (node.source != null) {
			Coord coord = new Coord(node.source.getGroupId(), node.source.getArtifactId());
			out.append(cache.artifactPath(coord, versions.get(coord)));
			out.append(":");
		}
		for (Node child: node.children) {
			buildClasspath(child, out);
		}
	}
	
	public String classpath() {
		StringBuilder s = new StringBuilder();
		buildClasspath(root, s);
		return s.toString();
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
	
}