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
import java.util.ArrayList;
import java.util.List;

class Cache {
	private final Path root;
	final HttpClient httpClient = HttpClient.newHttpClient();

	Cache(Path root) {
        if (root == null) {
			root = Path.of(System.getProperty("user.home"), ".m2", "repository");
		}
		this.root = root;
    }

	private Path groupDir(String groupId) {
		return root.resolve(groupId.replace('.', '/'));
	}

	private Path artifactDir(Coord coord) {
		return groupDir(coord.groupId).resolve(coord.artifactId);
	}
	
	private Path artifactDir(Coord coord, String version) {
		return artifactDir(coord).resolve(version);
	}

	public Path artifactPath(Coord coord, String version, String classifier, String type) {
		type = type == null ? "jar" : type;
		return artifactDir(coord, version).resolve(coord.artifactId + "-" + version +
				(classifier != null ? "-" + classifier : "") + "." + type);
	}
	
	private URI artifactUri(Coord coord, String version, String type) {
		type = type == null ? "jar" : type;
		return URI.create("https://repo1.maven.org/maven2/" + coord.groupId.replace('.', '/') + "/" + coord.artifactId + "/" + version + "/" + coord.artifactId + "-" + version + "." + type);
	}

	public Path metadataPath(Coord coord) {
		return artifactDir(coord).resolve("maven-metadata-central.xml");
	}

	private URI metadataUri(Coord coord) {
		return URI.create("https://repo1.maven.org/maven2/" + coord.groupId.replace('.', '/') + "/" + coord.artifactId + "/maven-metadata.xml");
	}

	public Path fetchMetadata(Coord coord) throws IOException {
		Path path = metadataPath(coord);
		if (!Files.exists(path) || System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis() > 24 * 60 * 60 * 1000) {
			URI uri = metadataUri(coord);
			Files.createDirectories(path.getParent());
			HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
			Path tmpFile = Path.of(path + ".tmp");
			try {
				System.out.println("Fetching " + uri);
				HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tmpFile));
				if (response.statusCode() == 200) {
					Files.move(tmpFile, path, StandardCopyOption.REPLACE_EXISTING);
				} else if (response.statusCode() != 404 || !Files.exists(path)) {
					throw new IOException("Unexpected status code: " + response.statusCode() + " for " + uri);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			} finally {
				Files.deleteIfExists(tmpFile);
			}
		}
		return path;
	}
	
	public Path fetch(Coord coord, String version, String classifier, String type) throws IOException {
		URI uri = artifactUri(coord, version, type);
		Path path = artifactPath(coord, version, classifier, type);
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
		return path;
	}
	
	String latestVersion(Coord coord) throws IOException, XMLStreamException {
		Path path = fetchMetadata(coord);
		if (!Files.exists(path)) {
			return null;
		}
		List<String> versions = new ArrayList<>();
		try (InputStream in = Files.newInputStream(path)) {
			XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(in));
			while (xml.hasNext()) {
				int event = xml.next();
				if (event == XMLStreamReader.START_ELEMENT && "version".equals(xml.getLocalName())) {
					versions.add(xml.getElementText());
				}
			}
		}
		Version latest = null;
		for (String v : versions) {
			Version version = new Version(v);
			if (version.isStable()) {
				if (latest == null || version.compareTo(latest) > 0) {
					latest = version;
				}
			}
		}
		return latest == null ? null : latest.toString();
	}

	Model readProject(Coord coord, String version) throws XMLStreamException, IOException {
		Path path = fetch(coord, version, null, "pom");
		try (InputStream in = Files.newInputStream(path)) {
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