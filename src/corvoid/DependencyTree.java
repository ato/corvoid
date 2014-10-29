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

class DependencyTree {
	Cache cache = new Cache();
	Map<Coord,String> versions = new HashMap<>();
	Set<Coord> unconstrained = new HashSet<>();
	Node root;
	
	class Node {
		Set<Coord> exclusions;
		int depth;
		Project project;
		Future<Project> future;
		Dependency source;
		List<Node> children;
		
		void resolve() throws XMLStreamException, IOException {
			children = new ArrayList<>();
			for (Dependency dep : project.dependencies) {
				Coord coord = dep.coord.unversioned();
				if (!exclusions.contains(coord) && !versions.containsKey(coord) && dep.scope.equals("compile") && !dep.optional) {
					if (dep.coord.version == null) {
						Dependency dm = project.dependencyManagement.get(coord);
						if (dm != null) {
							dep.coord.version = dm.coord.version;
						}
					}
					if (dep.coord.version == null) {
						unconstrained.add(coord);
					} else {
						unconstrained.remove(coord);
						versions.put(coord, dep.coord.version);
						Node node = new Node();
						node.depth = depth + 1;
						node.exclusions = new HashSet<>(exclusions);
						node.exclusions.addAll(dep.exclusions);
						node.future = cache.readAsync(dep.coord);
						node.source = dep;
					    //node.project = cache.readAndInheritProject(dep.coord);
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
			out.println(project);
		}
	}

	public void resolve(Project project) throws XMLStreamException, IOException {
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
			out.append(cache.artifactPath(node.source.coord));
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