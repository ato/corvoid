package corvoid;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

public class Corvoid {
	File pwd = new File(System.getProperty("user.dir"));

	public void newProject(String name) {
		File projectDir = new File(name);
		projectDir.mkdir();
		File pom = new File(projectDir, "pom.xml");
	}
	
	public DependencyTree tree() throws XMLStreamException, IOException {
		Project project = PomParser.parse(new FileInputStream("pom.xml"));
		Interpolator.interpolate(project);
		DependencyTree tree = new DependencyTree();
		tree.resolve(project);
		return tree;
	}
	
	public void run(String args[]) throws XMLStreamException, IOException {
		switch (args[0]) {
		case "new": newProject(args[1]); break;
		case "classpath": System.out.println(tree().classpath()); break;
		case "tree": tree().print(System.out); break;
		}
	}
	
	public static void main(String args[]) throws Exception {
		new Corvoid().run(args);
	}

}
