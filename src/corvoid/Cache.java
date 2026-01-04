package corvoid;

import corvoid.pom.Model;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cache for storing and retrieving artifacts from remote repositories.
 */
class Cache {
	private final Path root;
	private volatile HttpClient httpClient;
	private final Map<Path, CompletableFuture<Path>> pendingDownloads = new ConcurrentHashMap<>();

	Cache(Path root) {
        if (root == null) {
			root = Path.of(System.getProperty("user.home"), ".m2", "repository");
		}
		this.root = root;
    }

	private HttpClient httpClient() {
		if (httpClient == null) {
			synchronized (this) {
				if (httpClient == null) {
					httpClient = HttpClient.newBuilder().build();
				}
			}
		}
		return httpClient;
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
			return downloadIfMissing(path, metadataUri(coord), true);
		}
		return path;
	}
	
	public Path fetch(Coord coord, String version, String classifier, String type) throws IOException {
		return downloadIfMissing(artifactPath(coord, version, classifier, type), artifactUri(coord, version, type), false);
	}

	private Path downloadIfMissing(Path path, URI uri, boolean isMetadata) throws IOException {
		if (Files.exists(path) && !isMetadata) {
			return path;
		}
		CompletableFuture<Path> future = pendingDownloads.computeIfAbsent(path, p -> {
			try {
				Files.createDirectories(path.getParent());
			} catch (IOException e) {
				return CompletableFuture.failedFuture(e);
			}

			System.out.println("Fetching " + uri);
			HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
			Path tmpFile = Path.of(path + ".tmp");
			return httpClient().sendAsync(request, HttpResponse.BodyHandlers.ofFile(tmpFile))
					.thenApply(response -> {
						try {
							if (response.statusCode() == 200) {
								Files.move(tmpFile, path, StandardCopyOption.REPLACE_EXISTING);
							} else if (isMetadata && response.statusCode() == 404 && Files.exists(path)) {
								// Keep existing metadata if 404
							} else {
								throw new IOException("Unexpected status code: " + response.statusCode() + " for " + uri);
							}
							return path;
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						} finally {
							try {
								Files.deleteIfExists(tmpFile);
							} catch (IOException ignored) {
							}
							pendingDownloads.remove(path);
						}
					});
		});
		try {
			return future.get();
		} catch (Exception e) {
			throw new IOException(e);
		}
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
		return Model.read(path);
	}
}