package corvoid;

import corvoid.pom.Dependency;
import corvoid.pom.Model;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

class Cache {
	final File root = new File(new File(System.getProperty("user.home"), ".m2"), "repository");
	final HttpClient httpClient = HttpClient.newHttpClient();

	private File groupDir(String groupId) {
		return new File(root, groupId.replace('.', '/'));
	}
	
	private File artifactDir(Coord coord, String version) {
		return new File(new File(groupDir(coord.groupId), coord.artifactId), version);
	}

	public File artifactPath(Coord coord, String version, String classifier, String type) {
		type = type == null ? "jar" : type;
		return new File(artifactDir(coord, version), coord.artifactId + "-" + version +
				(classifier != null ? "-" + classifier : "") + "." + type);
	}
	
	private URI artifactUri(Coord coord, String version, String type) {
		type = type == null ? "jar" : type;
		return URI.create("https://repo1.maven.org/maven2/" + coord.groupId.replace('.', '/') + "/" + coord.artifactId + "/" + version + "/" + coord.artifactId + "-" + version + "." + type);
	}
	
	public File fetch(Coord coord, String version, String classifier, String type) throws IOException {
		URI uri = artifactUri(coord, version, type);
		Path path = artifactPath(coord, version, classifier, type).toPath();
		if (!Files.exists(path)) {
			Files.createDirectories(path.getParent());

			System.out.println("Fetching " + uri);
			HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
			Path tmpFile = Path.of(path + ".tmp");
			try {
				HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tmpFile));
				if (response.statusCode() != 200) {
					throw new IOException("Unexpected status code: " + response.statusCode() + " for " + uri);
				}
				Files.move(tmpFile, path, StandardCopyOption.REPLACE_EXISTING);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			} finally {
				Files.deleteIfExists(tmpFile);
			}
		}
		return path.toFile();
	}
	
	Model readProject(Coord coord, String version) throws XMLStreamException, IOException {
		File path = fetch(coord, version, null, "pom");
		try (InputStream in = new FileInputStream(path)) {
			XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(in));
			xml.nextTag();
			return new Model(xml);
		}
	}

	Model readAndInheritProject(Coord coord, String version) throws XMLStreamException, IOException {
		Model output = readProject(coord, version);
		Model project = output;
		while (project.getParent() != null && project.getParent().getArtifactId() != null) {
			project = readProject(new Coord(project.getParent().getGroupId(), project.getParent().getArtifactId()), project.getParent().getVersion());
			output = new Model(project, output);
		}
		Interpolator.interpolate(output);
		
		resolveImports(output);
		
		return output;
	}

	/**
	 * Resolves import dependencies in the dependencyManagement section by replacing the import with the contents of the
	 * imported project's dependencyManagement section.
	 */
	void resolveImports(Model output) throws XMLStreamException, IOException {
		List<Dependency> dependencies = output.getDependencyManagement().getDependencies();
		for (int i = 0; i < dependencies.size(); i++) {
			Dependency dep = dependencies.get(i);
			if ("import".equals(dep.getScope()) && "pom".equals(dep.getType())) {
				Model imported = readAndInheritProject(new Coord(dep.getGroupId(), dep.getArtifactId()), dep.getVersion());
				output.getDependencyManagement().getDependencies().addAll(i, imported.getDependencyManagement().getDependencies());
				i += imported.getDependencyManagement().getDependencies().size();
				output.getDependencyManagement().getDependencies().remove(i);
				i--;
			}
		}
	}
}