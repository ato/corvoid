package corvoid;

import corvoid.pom.Model;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

class Cache {
	final File root = new File(new File(System.getProperty("user.home"), ".m2"), "repository");
	
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
	
	private URL artifactUrl(Coord coord, String version, String type) {
		try {
			type = type == null ? "jar" : type;
			return new URL("http://repo1.maven.org/maven2/" + coord.groupId.replace('.', '/') + "/" + coord.artifactId + "/" + version + "/" + coord.artifactId + "-" + version + "." + type);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public File fetch(Coord coord, String version, String classifier, String type) throws IOException {
		URL url = artifactUrl(coord, version, type);
		File path = artifactPath(coord, version, classifier, type);
		if (!path.exists()) {
			path.getParentFile().mkdirs();

			System.out.println("Fetching " + url);
			try (InputStream in = url.openStream();
					OutputStream out = new FileOutputStream(path)) {
				byte[] buf = new byte[8192];
				for (;;) {
					int nbytes = in.read(buf);
					if (nbytes == -1)
						break;
					out.write(buf, 0, nbytes);
				}
			}
		}
		return path;
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
		return output;
	}
}