package corvoid;

import corvoid.pom.Dependency;
import corvoid.pom.Model;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Objects.requireNonNull;

public class Corvoid {
	
	final File projectRoot;
	
	public Corvoid() {
		this.projectRoot = new File(System.getProperty("user.dir"));
	}

	public Corvoid(File projectRoot) {
		this.projectRoot = projectRoot;
	}
	
	private String skeletonPom() throws IOException {
		try (Reader r = new InputStreamReader(requireNonNull(Corvoid.class.getResourceAsStream("skeleton.pom"),
				"Missing resource skeleton.pom"), UTF_8)) {
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

	private File target() {
		return new File(projectRoot, "target");
	}
	
	static String capify(String name) {
		StringBuilder buf = new StringBuilder();
		Matcher m = Pattern.compile("(?:^|[_-])(.)").matcher(name);
		int pos = 0;
		while (m.find()) {
			buf.append(name, pos, m.start());
			buf.append(m.group(1).toUpperCase());
			pos = m.end();
		}
		buf.append(name, pos, name.length());
		return buf.toString();
	}
	
	public void add(String dependency, String version) throws IOException, XMLStreamException {
		String[] parts = dependency.split(":");
		if (parts.length != 2) {
			System.err.println("Invalid dependency: " + dependency + ". Expected groupId:artifactId");
			System.exit(1);
		}
		String groupId = parts[0];
		String artifactId = parts[1];

		Model model = parseModel();
		Dependency existing = null;
		for (Dependency d : model.getDependencies()) {
			if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
				existing = d;
				break;
			}
		}

		String dependencyXml = String.format(
				"    <dependency>%n" +
				"            <groupId>%s</groupId>%n" +
				"            <artifactId>%s</artifactId>%n" +
				"            <version>%s</version>%n" +
				"        </dependency>%n    ",
				groupId, artifactId, version
		);


		if (existing != null) {
			System.out.println("Updating dependency " + groupId + ":" + artifactId + " from " + existing.getVersion() +
							   " to " + version);
			if (existing.versionStartOffset >= 0 && existing.versionEndOffset >= 0) {
				// Replace existing <version>...</version>
				splicePom(existing.versionStartOffset, "<version>" + version + "</version>",
						existing.versionEndOffset - existing.versionStartOffset);
			} else if (existing.artifactIdEndOffset >= 0) {
				// Insert <version> after <artifactId>
				splicePom(existing.artifactIdEndOffset, "\n            <version>" + version + "</version>", 0);
			} else {
				// Fallback: replace whole <dependency> block
				splicePom(existing.startOffset, dependencyXml, existing.endOffset - existing.startOffset);
			}
		} else {
			System.out.println("Adding dependency " + groupId + ":" + artifactId + " with version " + version);
			// Insert at end of dependencies
			int insertPosition = model.dependenciesEndOffset >= 0 ? model.dependenciesEndOffset : model.projectEndOffset;
			splicePom(insertPosition, dependencyXml, 0);
		}
	}

	private void splicePom(int offset, String insert, long skip) throws IOException {
		File pom = new File(projectRoot, "pom.xml");
		File tmp = new File(projectRoot, "pom.xml.tmp");
		byte[] buffer = new byte[8192];

		try (var in = new FileInputStream(pom);
			 var out = new FileOutputStream(tmp)) {

			int remaining = offset;
			int nread;
			while ((nread = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
				out.write(buffer, 0, nread);
				remaining -= nread;
			}

			out.write(insert.getBytes(UTF_8));

			while (skip > 0) {
				skip -= in.skip(skip);
			}

			while ((nread = in.read(buffer)) >= 0) {
				out.write(buffer, 0, nread);
			}
		}

		Files.move(tmp.toPath(), pom.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	public void newProject(String name) throws IOException {
		File projectDir = new File(name);
		if (projectDir.exists()) {
			System.err.println(projectDir + " already exists");
			System.exit(1);
		}
		File srcDir = new File(projectDir, "src");
		File srcPkgDir = new File(srcDir, name.replace("-", ""));
		File resDir = new File(projectDir, "resources");

		projectDir.mkdir();
		srcDir.mkdir();
		resDir.mkdir();
		srcPkgDir.mkdir();
		new File(resDir, name).mkdir();
		
		File pom = new File(projectDir, "pom.xml");
		try (Writer w = new FileWriter(pom)) {
			w.write(skeletonPom()
					.replace("$[name]", name));
		}
		
		String mainClass = capify(name);
		File mainClassFile = new File(srcPkgDir, mainClass + ".java");
		try (Writer w = new FileWriter(mainClassFile)) {
			w.write("package " + name + ";\n\npublic class " + mainClass + " {\n    public static void main(String args[]) {\n        System.out.println(\"Hello, world.\");\n    }\n}\n");
		}
	}
	
	public DependencyTree tree() throws XMLStreamException, IOException {
		Model project = parseModel();
		Interpolator.interpolate(project);
		DependencyTree tree = new DependencyTree();
		tree.resolve(project);
		return tree;
	}

	public Model parseModel() throws XMLStreamException, FactoryConfigurationError, FileNotFoundException {
		XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(new FileInputStream(new File(projectRoot, "pom.xml"))));
		xml.nextTag();
		return new Model(xml);
	}

	private void usage() {
		System.out.println("corvoid COMMAND");
		System.out.println("Fetch dependencies and build Java projects");
		System.out.println("\nCommands:");
		System.out.println("  add        - add a dependency to pom.xml");
		System.out.println("  classpath  - print the project's classpath");
		System.out.println("  clean      - delete the build target directory");
		System.out.println("  compile    - compile the project");
		System.out.println("  deps       - fetch dependencies");
		System.out.println("  jar        - build a jar file of classes and resources");
		System.out.println("  lint       - check for common problems");
		System.out.println("  new        - create a new project");
		System.out.println("  run        - run a class");
		System.out.println("  search     - search Maven Central for artifacts");
		System.out.println("  tree [-s]  - print a dependency tree");
		System.out.println("  uberjar    - build a standalone jar file");
		System.out.println("  watch      - watch for changes and recompile when seen");
		System.exit(1);
	}
	
	public void command(String[] args) throws XMLStreamException, IOException, InterruptedException {
		if (args.length == 0)
			usage();
		switch (args[0]) {
			case "add": add(args[1], args[2]); break;
			case "new": newProject(args[1]); break;
			case "clean": clean(); break;
			case "classpath": System.out.println(tree().classpath()); break;
			case "deps": tree().fetchDependencies(); break;
			case "search": search(args[1]); break;
			case "tree": tree().print(System.out, args.length > 1 && "-s".equals(args[1])); break;
			case "compile": compile(); break;
			case "run": run(args); break;
			case "jar": jar(); break;
			case "uberjar": uberjar(); break;
			case "watch": watch(); break;
			case "lint": lint(); break;
			default: usage();
		}
	}

	@SuppressWarnings("unchecked")
    public void search(String query) throws IOException {
		URL url = new URL("https://central.sonatype.com/api/internal/browse/components");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		try (OutputStream out = conn.getOutputStream()) {
			Json.write(out, Map.of("size", 10, "searchTerm", query, "filter", List.of()));
		}
		try (InputStream in = conn.getInputStream()) {
			var results = (Map<String, Object>) Json.read(in);
			var components = (List<Map<String, Object>>) results.get("components");
			for (var r : components) {
				var versionInfo = (Map<String,Object>)r.get("latestVersionInfo");
				var timestamp = Instant.ofEpochMilli((long)versionInfo.get("timestampUnixWithMS"));
				var id = String.format("\033[90m%s:\033[1;36m%s\033[0m \033[1;33m%s\033[0m \033[90m%s\033[0m", r.get("namespace"), r.get("name"),
						versionInfo.get("version"), timestamp.atZone(ZoneId.systemDefault()).toLocalDate());
				String description = (String)r.get("description");
				if (description == null) {
					System.out.println(id);
				} else {
					description = description.replaceAll("\\s+", " ");
					System.out.printf("%-100s # %s%n", id, description);
				}
			}
		}
	}

	private void clean() throws IOException {
		Files.walkFileTree(target().toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private void lint() throws IOException, XMLStreamException {
		boolean problems = lintDuplicateClasses();
		System.exit(problems ? 1 : 0);
	}

	private boolean lintDuplicateClasses() throws XMLStreamException, IOException {
		boolean problems = false;
		Map<String,String> classes = new HashMap<>();
		for (File jarFile : tree().classpathFiles()) {
			try (ZipFile zip = new ZipFile(jarFile)) {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String name = entry.getName();
					if (name.endsWith(".class")) {
						String prev = classes.put(name, jarFile.getName());
						if (prev != null) {
							System.out.println("Duplicate class: " + name + " (" + prev + ", " + jarFile.getName() + ")");
							problems = true;
						}
					}
				}
			} catch (IOException e) {
				System.out.println(e);
				problems = true;
			}
		}
		return problems;
	}

	private static String progressBar(long current, long total) {
		int size = 48;
		int progress = (int)(size * current / total);
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < progress; i++) {
			sb.append("=");
		}
		sb.append(">");
		for (int i = progress + 1; i < size; i++) {
			sb.append(" ");
		}
		sb.append("]");
		return sb.toString();
	}

	private static void clearLine() {
		System.out.print("\033[F\033[J");
	}

	List<File> dirsToIncludeInJar() {
		List<File> dirs = new ArrayList<>();
		for (String s : Arrays.asList("classes", "resources")) {
			File dir = new File(target(), s);
			if (dir.isDirectory()) {
				dirs.add(dir);
			}
		}
		return dirs;
	}

	void uberjar() throws IOException, XMLStreamException {
		Model model = parseModel();
		tree().fetchDependencies();
		File uberjarFile = new File(target(), model.getArtifactId() + "-" + model.getVersion() + "-standalone.jar");
		ensureTargetExists();
		try (JarWriter uberjar = new JarWriter(new FileOutputStream(uberjarFile))) {
			writeJarContents(model, uberjar);
			int progress = 0;
			List<File> files = tree().classpathFiles();
			for (File f : files) {
				System.out.println("Merging jars " + progressBar(progress++, files.size()) + " " + f.getName());
				try (ZipFile zf = new ZipFile(f)) {
					uberjar.putJarContents(zf);
				}
				clearLine();
			}
		}
	}

	private void ensureTargetExists() throws IOException {
		Files.createDirectories(target().toPath());
	}

	void jar() throws IOException, XMLStreamException {
		Model model = parseModel();
		File outFile = new File(target(), model.getArtifactId() + "-" + model.getVersion() + ".jar");
		ensureTargetExists();
		try (JarWriter jar = new JarWriter(new FileOutputStream(outFile))) {
			writeJarContents(model, jar);
		}
	}

	private void writeJarContents(Model model, JarWriter jar) throws IOException, XMLStreamException {
		compile();
		jar.writeManifest(model.getBuild().getMainClass());
		for (File dir : dirsToIncludeInJar()) {
			jar.putDirContents(dir);
		}
	}

	private void run(String[] args) throws XMLStreamException, IOException {
		if (args.length < 2) {
			System.err.println("Usage: corvoid run main-class args...");
			System.exit(1);
		}
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
		boolean verbose = false;
		
		List<File> walkSources(File srcDir) {
			List<File> list = new ArrayList<>();
			LinkedList<File> queue = new LinkedList<>();
			queue.add(srcDir.getAbsoluteFile());
			while (!queue.isEmpty()) {
				File dir = queue.remove();
				for (File file : requireNonNull(dir.listFiles())) {
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
		Model project = new Model(superPom(), parseModel());
		Interpolator.interpolate(project);
		DependencyTree tree = new DependencyTree();
		tree.resolve(project);
		CompilerOptions options = new CompilerOptions();
		options.classpath = tree.classpath();
		String srcDir = project.getBuild().getSourceDirectory();
		options.srcDir = new File(srcDir != null ? srcDir : "src");
		String outDir = project.getBuild().getOutputDirectory();
		options.outDir = new File(outDir != null ? outDir : "target/classes");
		return options;
	}
	
	private void compile() throws XMLStreamException, IOException {
		CompilerOptions options = buildCompilerOptions();
		System.out.println("Compiling");
		compileViaToolApi(options);
		clearLine();
	}

	private void compileViaToolApi(CompilerOptions options) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		List<String> cmd = options.buildCommandLine();
		cmd.remove(0); // drop javac
		compiler.run(null, null, null, cmd.toArray(new String[0]));
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
				Path filename = dir.resolve((Path) event.context());
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

	public static void main(String[] args) throws Exception {
		new Corvoid().command(args);
	}

}
