package corvoid;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardWatchEventKinds.*;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import corvoid.pom.Model;

public class Corvoid {
	
	final File projectRoot;
	
	public Corvoid() {
		this.projectRoot = new File(System.getProperty("user.dir"));
	}

	public Corvoid(File projectRoot) {
		this.projectRoot = projectRoot;
	}
	
	private String skeletonPom() throws IOException {
		try (Reader r = new InputStreamReader(Corvoid.class.getResourceAsStream("skeleton.pom"), UTF_8)) {			
			StringBuilder sb = new StringBuilder();
			char[] b = new char[8192];
			for (int nread = r.read(b); nread >= 0; nread = r.read(b)) {
				sb.append(b, 0, nread);
			}
			return sb.toString();
		}
	}
	
	private static Model superPom;
	
	private Model superPom() {
		if (superPom == null) {
			try (InputStream in = Corvoid.class
					.getResourceAsStream("super.pom")) {
				XMLStreamReader xml = XMLInputFactory.newInstance()
						.createXMLStreamReader(new StreamSource(in));
				xml.nextTag();
				superPom = new Model(xml);
			} catch (XMLStreamException | FactoryConfigurationError
					| IOException e) {
				throw new RuntimeException(e);
			}
		}
		return superPom;
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
			w.write(skeletonPom()
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
	
	public Model parse() throws XMLStreamException, FactoryConfigurationError, FileNotFoundException {
		XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(new FileInputStream(new File(projectRoot, "pom.xml"))));
		xml.nextTag();
		return new Model(xml);
	}
	
	public void command(String args[]) throws XMLStreamException, IOException, InterruptedException {
		switch (args[0]) {
		case "new": newProject(args[1]); break;
		case "classpath": System.out.println(tree().classpath()); break;
		case "deps": tree().fetchDependencies(); break;
		case "tree": tree().print(System.out); break;
		case "compile": compile(); break;
		case "run": run(args); break;
		case "watch": watch(); break;
		}
	}

	private void run(String[] args) throws XMLStreamException, IOException {
		DependencyTree tree = tree();
		String classpath = tree.classpath();
		List<String> command = new ArrayList<>();
		command.add("java");
		command.add("-cp");
		command.add("target/classes:" + classpath);
		command.addAll(Arrays.asList(args).subList(1, args.length));
		try {
			new ProcessBuilder().command(command)
			.redirectError(Redirect.INHERIT)
			.redirectOutput(Redirect.INHERIT)
			.redirectInput(Redirect.INHERIT)
			.start().waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static class CompilerOptions {
		File srcDir, outDir;
		String classpath;
		boolean verbose = true;
		
		List<File> walkSources(File srcDir) {
			List<File> list = new ArrayList<>();
			LinkedList<File> queue = new LinkedList<>();
			queue.add(srcDir.getAbsoluteFile());
			while (!queue.isEmpty()) {
				File dir = queue.remove();
				for (File file : dir.listFiles()) {
					if (file.isDirectory()) {
						queue.add(file.getAbsoluteFile());
					} else if (file.toString().endsWith(".java")) {
						list.add(file);
					}
				}
			}
			return list;
		}


		List<String> buildCommandLine() {
			List<String> cmd = new ArrayList<>();
			cmd.add("javac");
			if (verbose) {
				cmd.add("-verbose");
			}
			cmd.add("-sourcepath");
			cmd.add(srcDir.toString());
			cmd.add("-cp");
			cmd.add(classpath);
			cmd.add("-d");
			cmd.add(outDir.toString());
			for (File f : walkSources(srcDir)) {
				cmd.add(f.toString());
			}
			return cmd;
		}
	}

	private CompilerOptions buildCompilerOptions() throws IOException, XMLStreamException {
		Model project = new Model(superPom(), parse());
		Interpolator.interpolate(project);
		DependencyTree tree = new DependencyTree();
		tree.resolve(project);
		CompilerOptions options = new CompilerOptions();
		options.classpath = tree.classpath();
		options.srcDir = new File(project.getBuild().getSourceDirectory());
		if (options.srcDir == null) {
			options.srcDir = new File("src");
		}
		options.outDir = new File(project.getBuild().getOutputDirectory());
		if (options.outDir == null) {
			options.outDir = new File("target/classes");
		}
		return options;
	}
	
	private void compile() throws XMLStreamException, IOException {
		CompilerOptions options = buildCompilerOptions();
		compileViaToolApi(options);
	}

	private void compileViaToolApi(CompilerOptions options) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		List<String> cmd = options.buildCommandLine();
		cmd.remove(0); // drop javac
		compiler.run(null, null, null, cmd.toArray(new String[cmd.size()]));
	}

	private void compileExternal(CompilerOptions options) throws IOException {
		List<String> cmd = options.buildCommandLine();
		for (String s : cmd) {
			System.out.print(s);
			System.out.print(" ");
		}
		System.out.println();
		try {
			new ProcessBuilder().command(cmd)
					.redirectError(Redirect.INHERIT)
					.redirectOutput(Redirect.INHERIT)
					.start().waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void watch() throws IOException, XMLStreamException, InterruptedException {
		CompilerOptions options = buildCompilerOptions();
		final WatchService watcher = FileSystems.getDefault().newWatchService();
		final AtomicLong dirCount = new AtomicLong(0);
		Files.walk(options.srcDir.toPath()).forEach(new Consumer<Path>() {
			@Override
			public void accept(Path path) {
				try {
					if (!Files.isDirectory(path))
						return;
					path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
					dirCount.incrementAndGet();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		});

		System.out.println("Watching " + dirCount.get() + " directories");

		for (;;) {
			WatchKey key = watcher.take();
			Path dir = (Path) key.watchable();

			boolean recompile = false;

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if (kind == OVERFLOW) {
					recompile = true;
					break;
				}
				Path filename = dir.resolve(((WatchEvent<Path>)event).context());
				if (filename.toString().endsWith(".java")) {
					recompile = true;
				} else if (kind == ENTRY_CREATE && Files.isDirectory(filename)) {
					filename.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
				}
			}

			if (recompile) {
				compileViaToolApi(options);
			}

			key.reset();
		}
	}

	public static void main(String args[]) throws Exception {
		new Corvoid().command(args);
	}

}
