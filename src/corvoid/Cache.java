package corvoid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import corvoid.pom.Model;

class Cache {
	//ExecutorService threadPool = Executors.newFixedThreadPool(8);
	final File root = new File(new File(System.getProperty("user.home"), ".m2"), "repositoryx");
	
	private File groupDir(String groupId) {
		return new File(root, groupId.replace('.', '/'));
	}
	
	private File artifactDir(Coord coord, String version) {
		return new File(new File(groupDir(coord.groupId), coord.artifactId), version);
	}
	
	public File artifactPath(Coord coord, String version, String type) {
		type = type == null ? "jar" : type;
		return new File(artifactDir(coord, version), coord.artifactId + "-" + version + "." + type);
	}
	
	private URL artifactUrl(Coord coord, String version, String type) {
		try {
			type = type == null ? "jar" : type;
			return new URL("http://repo1.maven.org/maven2/" + coord.groupId.replace('.', '/') + "/" + coord.artifactId + "/" + version + "/" + coord.artifactId + "-" + version + "." + type);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public File fetch(Coord coord, String version, String type) throws IOException {
		URL url = artifactUrl(coord, version, type);
		File path = artifactPath(coord, version, type);
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
		File path = fetch(coord, version, "pom");
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
	
	/*
	
	Future<Model> readAsync(final Coord coord, final String version) {
		return threadPool.submit(new Callable<Model>() {

			@Override
			public Model call() throws Exception {
				return readAndInheritProject(coord, version);
			}
			 
		});
	}
	*/
}