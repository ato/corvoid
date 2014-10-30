package corvoid;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import corvoid.pom.Model;

public class Corvoid {
	private String defaultPom() throws IOException {
		try (Reader r = new InputStreamReader(Corvoid.class.getResourceAsStream("default.pom"), UTF_8)) {			
			StringBuilder sb = new StringBuilder();
			char[] b = new char[8192];
			for (int nread = r.read(b); nread >= 0; nread = r.read(b)) {
				sb.append(b, 0, nread);
			}
			return sb.toString();
		}
	}
	
	private String javaMinorVersion() {
		return System.getProperty("java.version").replaceFirst("^(\\d+\\.\\d+)\\..*", "$1");
	}
	
	private String capFirst(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
	
	public void newProject(String name) throws IOException {
		File projectDir = new File(name);
		if (projectDir.exists()) {
			System.err.println(projectDir + " already exists");
			System.exit(1);
		}
		File srcDir = new File(projectDir, "src");
		File srcPkgDir = new File(srcDir, name);
		File resDir = new File(projectDir, "resources");

		projectDir.mkdir();
		srcDir.mkdir();
		resDir.mkdir();
		srcPkgDir.mkdir();
		new File(resDir, name).mkdir();
		
		File pom = new File(projectDir, "pom.xml");
		try (Writer w = new FileWriter(pom)) {
			w.write(defaultPom()
					.replace("$[name]", name)
					.replace("$[java.version]", javaMinorVersion()));
		}
		
		String mainClass = capFirst(name);
		File mainClassFile = new File(srcPkgDir, mainClass + ".java");
		try (Writer w = new FileWriter(mainClassFile)) {
			w.write("public class " + mainClass + " {\n    public static void main(String args[]) {\n        System.out.println(\"Hello, world.\");\n    }\n}\n");
		}
	}
	
	public DependencyTree tree() throws XMLStreamException, IOException {
		Model project = parse();
		Interpolator.interpolate(project);
		DependencyTree tree = new DependencyTree();
		tree.resolve(project);
		return tree;
	}
	
	public Model parse() throws XMLStreamException, FactoryConfigurationError {
		XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(new File("pom.xml")));
		xml.nextTag();
		return new Model(xml);
	}
	
	public void run(String args[]) throws XMLStreamException, IOException {
		switch (args[0]) {
		case "new": newProject(args[1]); break;
		case "classpath": System.out.println(tree().classpath()); break;
		case "tree": tree().print(System.out); break;
		case "compile": compile(); break;
		}
	}

	private void compile() throws XMLStreamException, IOException {
		Model project = parse();
		Interpolator.interpolate(project);
		DependencyTree tree = new DependencyTree();
		tree.resolve(project);
		String srcDir = project.getBuild().getSourceDirectory();
		if (srcDir == null) {
			srcDir = "src";
		}
		String outDir = project.getBuild().getOutputDirectory();
		if (outDir == null) {
			outDir = "target/classes";
		}
		new ProcessBuilder().command("javac", "-sourcepath", srcDir, "-cp", tree.classpath(), "-d", outDir);
		
	}

	public static void main(String args[]) throws Exception {
		new Corvoid().run(args);
	}

}
