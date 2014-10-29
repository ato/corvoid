package corvoid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.stream.XMLStreamException;

class Cache {
	ExecutorService threadPool = Executors.newFixedThreadPool(8);
	final File root = new File(new File(System.getProperty("user.home"), ".m2"), "repositoryx");
	
	private File groupDir(String groupId) {
		return new File(root, groupId.replace('.', '/'));
	}
	
	private File artifactDir(VersionedCoord coord) {
		coord.validate();
		return new File(new File(groupDir(coord.groupId), coord.artifactId), coord.version);
	}
	
	public File artifactPath(VersionedCoord coord) {
		coord.validate();
		return new File(artifactDir(coord), coord.artifactId + "-" + coord.version + "." + coord.type);
	}
	
	private File pomPath(VersionedCoord coord) {
		coord.validate();
		return new File(artifactDir(coord), coord.artifactId + "-" + coord.version + ".pom");
	}
	private URL pomUrl(VersionedCoord coord) {
		coord.validate();
		try {
			return new URL("http://repo1.maven.org/maven2/" + coord.groupId.replace('.', '/') + "/" + coord.artifactId + "/" + coord.version + "/" + coord.artifactId + "-" + coord.version + ".pom");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	Project readProject(VersionedCoord coord) throws XMLStreamException, IOException {
		File path = pomPath(coord);
		if (!path.exists()) {
			path.getParentFile().mkdirs();
			URL url = pomUrl(coord);
			System.out.println("Fetching " + url);
			try (InputStream in = url.openStream();
					OutputStream out = new FileOutputStream(path)) {
				byte[] buf = new byte[8192];
				for (;;) {
					int nbytes = in.read(buf);
					if (nbytes == -1) break;
					out.write(buf, 0, nbytes);
				}
			}
		}
		try (InputStream in = new FileInputStream(pomPath(coord))) {
			return PomParser.parse(in);
		}
	}

	Project readAndInheritProject(VersionedCoord coord) throws XMLStreamException, IOException {
		Project output = readProject(coord);
		Project project = output;
		while (project.parent != null) {
			project = readProject(project.parent);
			output.inherit(project);
		}
		Interpolator.interpolate(output);
		return output;
	}
	
	Future<Project> readAsync(final VersionedCoord coord) {
		return threadPool.submit(new Callable<Project>() {

			@Override
			public Project call() throws Exception {
				return readAndInheritProject(coord);
			}
			 
		});
	}
}