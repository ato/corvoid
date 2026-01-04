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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Objects.requireNonNull;

public class Corvoid {
	
	final Path projectRoot;
	private final Workspace workspace;

	public Corvoid() {
		this(Path.of(System.getProperty("user.dir")), null);
	}

	public Corvoid(Path projectRoot) {
		this(projectRoot, null);
	}

	public Corvoid(File projectRoot) {
		this(projectRoot.toPath(), null);
	}

	public Corvoid(Path projectRoot, Path repositoryRoot) {
		this.projectRoot = projectRoot;
		this.workspace = new Workspace(new Cache(repositoryRoot));
	}

	public Corvoid(File projectRoot, File repositoryRoot) {
		this(projectRoot.toPath(), repositoryRoot == null ? null : repositoryRoot.toPath());
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

	private Path target() {
		return projectRoot.resolve("target");
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
		Dependency existing = findDependency(model, groupId, artifactId);

		if (existing != null) {
			updateDependency(existing, version);
		} else {
			String dependencyXml = String.format(
					"    <dependency>%n" +
					"            <groupId>%s</groupId>%n" +
					"            <artifactId>%s</artifactId>%n" +
					"            <version>%s</version>%n" +
					"        </dependency>%n    ",
					groupId, artifactId, version
			);
			System.out.println("Adding dependency " + groupId + ":" + artifactId + " with version " + version);
			// Insert at end of dependencies
			int insertPosition = model.dependenciesEndOffset >= 0 ? model.dependenciesEndOffset : model.projectEndOffset;
			splicePom(insertPosition, dependencyXml, 0);
		}
	}

	private Dependency findDependency(Model model, String groupId, String artifactId) {
		for (Dependency d : model.getDependencies()) {
			if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
				return d;
			}
		}
		if (model.getDependencyManagement() != null) {
			for (Dependency d : model.getDependencyManagement().getDependencies()) {
				if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
					return d;
				}
			}
		}
		return null;
	}

	private void updateDependency(Dependency existing, String version) throws IOException {
		System.out.println("Updating dependency " + existing.getGroupId() + ":" + existing.getArtifactId() + " from " + existing.getVersion() +
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
			String dependencyXml = String.format(
					"    <dependency>%n" +
					"            <groupId>%s</groupId>%n" +
					"            <artifactId>%s</artifactId>%n" +
					"            <version>%s</version>%n" +
					"        </dependency>%n    ",
					existing.getGroupId(), existing.getArtifactId(), version
			);
			splicePom(existing.startOffset, dependencyXml, existing.endOffset - existing.startOffset);
		}
	}

	private void splicePom(int offset, String insert, long skip) throws IOException {
		Path pom = projectRoot.resolve("pom.xml");
		Path tmp = projectRoot.resolve("pom.xml.tmp");
		char[] buffer = new char[8192];

		if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");
		if (skip < 0) throw new IllegalArgumentException("skip must be >= 0");
		if (offset + skip > Files.size(pom)) throw new IllegalArgumentException("offset + skip must be <= file size");

		try (var in = Files.newBufferedReader(pom);
			 var out = Files.newBufferedWriter(tmp)) {

			int remaining = offset;
			int nread;
			while ((nread = in.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
				out.write(buffer, 0, nread);
				remaining -= nread;
			}

			out.write(insert);

			while (skip > 0) {
				long n = in.skip(skip);
				if (n == 0) throw new EOFException("Unexpected EOF");
				skip -= n;
			}

			while ((nread = in.read(buffer)) >= 0) {
				out.write(buffer, 0, nread);
			}
		}

		Files.move(tmp, pom, StandardCopyOption.REPLACE_EXISTING);
	}

	public void newProject(String name) throws IOException {
		Path projectDir = Path.of(name);
		if (Files.exists(projectDir)) {
			System.err.println(projectDir + " already exists");
			System.exit(1);
		}
		Path srcDir = projectDir.resolve("src");
		Path srcPkgDir = srcDir.resolve(name.replace("-", ""));
		Path resDir = projectDir.resolve("resources");

		Files.createDirectories(srcPkgDir);
		Files.createDirectories(resDir.resolve(name));
		
		Path pom = projectDir.resolve("pom.xml");
		try (Writer w = Files.newBufferedWriter(pom)) {
			w.write(skeletonPom()
					.replace("$[name]", name));
		}
		
		String mainClass = capify(name);
		Path mainClassFile = srcPkgDir.resolve(mainClass + ".java");
		try (Writer w = Files.newBufferedWriter(mainClassFile)) {
			w.write("package " + name + ";\n\npublic class " + mainClass + " {\n    public static void main(String args[]) {\n        System.out.println(\"Hello, world.\");\n    }\n}\n");
		}
	}
	
	public DependencyTree tree() throws XMLStreamException, IOException {
		Model project = parseModel();
		Path currentRoot = projectRoot;
		if (project.getParent() != null && project.getParent().getArtifactId() != null) {
			String relativePath = project.getParent().getRelativePath();
			if (relativePath == null) relativePath = "../pom.xml";
			Path parentPom = projectRoot.resolve(relativePath).normalize();
			Model parent = Model.read(parentPom);
			project = new Model(parent, project);
			currentRoot = parentPom.getParent();
		}
		workspace.scanModules(currentRoot);
		Interpolator.interpolate(project);
		DependencyTree tree = new DependencyTree(workspace);
		tree.resolve(project);
		return tree;
	}

	public Model parseModel() throws XMLStreamException, FactoryConfigurationError, IOException {
		return Model.read(projectRoot.resolve("pom.xml"));
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
		System.out.println("  outdated   - check for newer versions of dependencies");
		System.out.println("  run        - run a class");
		System.out.println("  search     - search Maven Central for artifacts");
		System.out.println("  test       - run unit tests");
		System.out.println("  tree [-s]  - print a dependency tree");
		System.out.println("  uberjar    - build a standalone jar file");
		System.out.println("  update     - update dependencies to latest stable versions");
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
			case "tree": printTree(args); break;
			case "compile": compile(); break;
			case "test": test(args); break;
			case "run": run(args); break;
			case "jar": jar(); break;
			case "uberjar": uberjar(); break;
			case "watch": watch(); break;
			case "lint": lint(); break;
			case "outdated": outdated(); break;
			case "update": update(args); break;
			default: usage();
		}
	}

	private void printTree(String[] args) throws XMLStreamException, IOException {
		boolean sort = false, showGroupId = false;
		for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-s" -> sort = true;
				case "-g" -> showGroupId = true;
                default -> {
					System.err.println("Unknown option: " + args[i]);
					System.err.println("""
							Usage: corvoid tree [-s] [-g]
							Print a dependency tree
							
							-s Sort by size
							-g Show group IDs
							""");
                    System.exit(1);
                }
            }
		}
		tree().print(System.out, sort, showGroupId);
	}

	@SuppressWarnings("unchecked")
	public void search(String query) throws IOException {
		HttpClient client = HttpClient.newHttpClient();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Json.write(out, Map.of("size", 10, "searchTerm", query, "filter", List.of()));
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://central.sonatype.com/api/internal/browse/components"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
				.build();
		try {
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				throw new IOException("Unexpected status code: " + response.statusCode());
			}
			try (InputStream in = response.body()) {
				var results = (Map<String, Object>) Json.read(in);
				var components = (List<Map<String, Object>>) results.get("components");
				for (var r : components) {
					var versionInfo = (Map<String, Object>) r.get("latestVersionInfo");
					var timestamp = Instant.ofEpochMilli((long) versionInfo.get("timestampUnixWithMS"));
					var id = String.format("\033[90m%s:\033[1;36m%s\033[0m \033[1;33m%s\033[0m \033[90m%s\033[0m", r.get("namespace"), r.get("name"),
							versionInfo.get("version"), timestamp.atZone(ZoneId.systemDefault()).toLocalDate());
					String description = (String) r.get("description");
					if (description == null) {
						System.out.println(id);
					} else {
						description = description.replaceAll("\\s+", " ");
						System.out.printf("%-100s # %s%n", id, description);
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	public void outdated() throws XMLStreamException, IOException {
		Model model = parseModel();
		Interpolator.interpolate(model);
		workspace.resolveImports(model);
		List<String> results = Collections.synchronizedList(new ArrayList<>());
		model.getDependencies().parallelStream().forEach(dep -> {
			try {
				Coord coord = new Coord(dep.getGroupId(), dep.getArtifactId());
				String version = dep.getVersion();
				if (version == null) {
					version = model.findManagedVersion(dep);
				}
				String latest = workspace.getCache().latestVersion(coord);
				if (latest != null && !latest.equals(version)) {
					results.add(coord + " " + version + " -> " + latest);
				}
			} catch (Exception e) {
				System.err.println("Error checking " + dep.getGroupId() + ":" + dep.getArtifactId() + ": " + e.getMessage());
			}
		});
		Collections.sort(results);
		for (String res : results) {
			System.out.println(res);
		}
	}

	public void update(String[] args) throws XMLStreamException, IOException {
		Set<String> toUpdate = new HashSet<>(Arrays.asList(args).subList(1, args.length));

		// Pass 1: Update dependencyManagement
		Model model = parseModel();
		Interpolator.interpolate(model);
		if (model.getDependencyManagement() != null) {
			record Update(Dependency dep, String version) {}
			List<Update> updates = new ArrayList<>();
			for (Dependency dep : model.getDependencyManagement().getDependencies()) {
				String coordStr = dep.getGroupId() + ":" + dep.getArtifactId();
				if (toUpdate.isEmpty() || toUpdate.contains(coordStr)) {
					try {
						Coord coord = new Coord(dep.getGroupId(), dep.getArtifactId());
						String version = dep.getVersion();
						String latest = workspace.getCache().latestVersion(coord);
						if (latest != null && !latest.equals(version)) {
							updates.add(new Update(dep, latest));
						}
					} catch (Exception e) {
						System.err.println("Error updating " + coordStr + ": " + e.getMessage());
					}
				}
			}
			if (!updates.isEmpty()) {
				updates.sort(Comparator.comparingInt((Update u) -> u.dep.startOffset).reversed());
				for (Update u : updates) {
					updateDependency(u.dep, u.version);
				}
			}
		}

		// Pass 2: Update dependencies
		model = parseModel();
		Interpolator.interpolate(model);
		workspace.resolveImports(model);

		record Update(Dependency dep, String version) {}
		List<Update> updates = new ArrayList<>();

		for (Dependency dep : model.getDependencies()) {
			String coordStr = dep.getGroupId() + ":" + dep.getArtifactId();
			if (toUpdate.isEmpty() || toUpdate.contains(coordStr)) {
				try {
					Coord coord = new Coord(dep.getGroupId(), dep.getArtifactId());
					String version = dep.getVersion();
					if (version == null) {
						version = model.findManagedVersion(dep);
					}
					String latest = workspace.getCache().latestVersion(coord);
					if (latest != null && !latest.equals(version)) {
						updates.add(new Update(dep, latest));
					}
				} catch (Exception e) {
					System.err.println("Error updating " + coordStr + ": " + e.getMessage());
				}
			}
		}

		if (!updates.isEmpty()) {
			updates.sort(Comparator.comparingInt((Update u) -> u.dep.startOffset).reversed());
			for (Update u : updates) {
				updateDependency(u.dep, u.version);
			}
		}
	}

	private void clean() throws IOException {
		Files.walkFileTree(target(), new SimpleFileVisitor<Path>() {
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
		for (Path jarPath : tree().classpathFiles()) {
			try (ZipFile zip = new ZipFile(jarPath.toFile())) {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String name = entry.getName();
					if (name.endsWith(".class")) {
						String prev = classes.put(name, jarPath.getFileName().toString());
						if (prev != null) {
							System.out.println("Duplicate class: " + name + " (" + prev + ", " + jarPath.getFileName().toString() + ")");
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

	List<Path> dirsToIncludeInJar() {
		List<Path> dirs = new ArrayList<>();
		for (String s : Arrays.asList("classes", "resources")) {
			Path dir = target().resolve(s);
			if (Files.isDirectory(dir)) {
				dirs.add(dir);
			}
		}
		return dirs;
	}

	void uberjar() throws IOException, XMLStreamException {
		Model model = parseModel();
		tree().fetchDependencies();
		Path uberjarFile = target().resolve(model.getArtifactId() + "-" + model.getVersion() + "-standalone.jar");
		ensureTargetExists();
		try (JarWriter uberjar = new JarWriter(Files.newOutputStream(uberjarFile))) {
			writeJarContents(model, uberjar);
			int progress = 0;
			List<Path> files = tree().classpathFiles();
			for (Path f : files) {
				System.out.println("Merging jars " + progressBar(progress++, files.size()) + " " + f.getFileName().toString());
				try (ZipFile zf = new ZipFile(f.toFile())) {
					uberjar.putJarContents(zf);
				}
				clearLine();
			}
		}
	}

	private void ensureTargetExists() throws IOException {
		Files.createDirectories(target());
	}

	void jar() throws IOException, XMLStreamException {
		Model model = parseModel();
		Path outFile = target().resolve(model.getArtifactId() + "-" + model.getVersion() + ".jar");
		ensureTargetExists();
		try (JarWriter jar = new JarWriter(Files.newOutputStream(outFile))) {
			writeJarContents(model, jar);
		}
	}

	private void writeJarContents(Model model, JarWriter jar) throws IOException, XMLStreamException {
		compile();
		jar.writeManifest(model.getBuild().getMainClass());
		for (Path dir : dirsToIncludeInJar()) {
			jar.putDirContents(dir);
		}
	}

	private void run(String[] args) throws XMLStreamException, IOException {
		DependencyTree tree = tree();
		String classpath = tree.classpath();
		List<String> command = new ArrayList<>();
		command.add("java");
		command.add("-cp");
		command.add("target/classes:" + classpath);
		if (args.length > 1 && !args[1].equals("--")) {
			command.addAll(Arrays.asList(args).subList(1, args.length));
		} else {
			Model model = parseModel();
			String mainClass = model.getBuild().getMainClass();
			if (mainClass == null) {
				System.err.println("No main class specified in pom.xml");
				System.err.println("Use: corvoid run <main-class> args...");
				System.exit(1);
			}
			command.add(mainClass);
			if (args.length > 1) command.addAll(Arrays.asList(args).subList(2, args.length));
		}
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
		Path srcDir, outDir;
		String classpath;
		boolean verbose = false;
		boolean junit5 = false;
		
		List<Path> walkSources(Path srcDir) throws IOException {
			List<Path> list = new ArrayList<>();
			try (Stream<Path> stream = Files.walk(srcDir)) {
				stream.filter(p -> Files.isRegularFile(p))
						.filter(p -> p.toString().endsWith(".java"))
						.forEach(p -> list.add(p));
			}
			return list;
		}


		List<String> buildCommandLine() throws IOException {
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
			for (Path f : walkSources(srcDir)) {
				cmd.add(f.toString());
			}
			return cmd;
		}
	}

	private CompilerOptions buildCompilerOptions() throws IOException, XMLStreamException {
		return buildCompilerOptions(false);
	}

	private CompilerOptions buildCompilerOptions(boolean test) throws IOException, XMLStreamException {
		CompilerOptions options = new CompilerOptions();
		Model project = new Model(superPom(), parseModel());
		Interpolator.interpolate(project);
		if (test) {
			options.junit5 = injectJUnit5ConsoleRunner(project);
		}
		DependencyTree tree = new DependencyTree(workspace);
		tree.resolve(project);
		tree.fetchDependencies();
		options.classpath = tree.classpath();
		if (test) {
			String srcDir = project.getBuild().getTestSourceDirectory();
			options.srcDir = Path.of(srcDir != null ? srcDir : "test");
			String outDir = project.getBuild().getTestOutputDirectory();
			options.outDir = Path.of(outDir != null ? outDir : "target/test-classes");
			String mainOutDir = project.getBuild().getOutputDirectory();
			options.classpath = (mainOutDir != null ? mainOutDir : "target/classes") + ":" + options.classpath;
		} else {
			String srcDir = project.getBuild().getSourceDirectory();
			options.srcDir = Path.of(srcDir != null ? srcDir : "src");
			String outDir = project.getBuild().getOutputDirectory();
			options.outDir = Path.of(outDir != null ? outDir : "target/classes");
		}
		return options;
	}

	private void compile() throws XMLStreamException, IOException {
		CompilerOptions options = buildCompilerOptions();
		if (!Files.exists(options.outDir)) {
			Files.createDirectories(options.outDir);
		}
		System.out.println("Compiling");
		compileViaToolApi(options);
		clearLine();
	}

	private void compileTests() throws XMLStreamException, IOException {
		CompilerOptions options = buildCompilerOptions(true);
		if (!Files.exists(options.outDir)) {
			Files.createDirectories(options.outDir);
		}
		System.out.println("Compiling tests");
		compileViaToolApi(options);
		clearLine();
	}

	private void test(String[] args) throws XMLStreamException, IOException {
		compile();
		compileTests();
		CompilerOptions options = buildCompilerOptions(true);
		String classpath = options.outDir + ":" + options.classpath;
		List<String> testClasses = findTestClasses(options.outDir);
		if (testClasses.isEmpty()) {
			System.out.println("No tests found");
			return;
		}

		List<String> command = new ArrayList<>();
		command.add("java");
		command.add("-cp");
		command.add(classpath);

		if (options.junit5) {
			command.add("org.junit.platform.console.ConsoleLauncher");
			command.add("execute");
			command.add("--scan-class-path");
			command.add("--disable-banner");
            command.addAll(Arrays.asList(args).subList(1, args.length));
		} else {
			command.add("org.junit.runner.JUnitCore");
			command.addAll(testClasses);
		}

		try {
			new ProcessBuilder().command(command)
					.redirectError(Redirect.INHERIT)
					.redirectOutput(Redirect.INHERIT)
					.start().waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean injectJUnit5ConsoleRunner(Model project) {
		boolean hasJUnit5 = false;
		boolean hasConsoleRunner = false;
		String junitPlatformVersion = "1.10.0"; // Default

		for (Dependency dep : project.getDependencies()) {
			String gid = dep.getGroupId();
			String aid = dep.getArtifactId();
			if (gid != null && (gid.equals("org.junit.jupiter") || gid.equals("org.junit.platform"))) {
				hasJUnit5 = true;
				if (aid != null && (aid.equals("junit-platform-console-standalone") || aid.equals("junit-platform-console"))) {
					hasConsoleRunner = true;
				}
				if (dep.getVersion() != null) {
					junitPlatformVersion = dep.getVersion();
				}
			}
		}

		if (hasJUnit5 && !hasConsoleRunner) {
			Dependency console = new Dependency();
			console.setGroupId("org.junit.platform");
			console.setArtifactId("junit-platform-console");
			console.setVersion(junitPlatformVersion);
			console.setScope("test");
			project.getDependencies().add(console);
		}
		return hasJUnit5;
	}

	private List<String> findTestClasses(Path testOutDir) throws IOException {
		List<String> testClasses = new ArrayList<>();
		if (!Files.exists(testOutDir)) {
			return testClasses;
		}
		Files.walkFileTree(testOutDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String name = file.getFileName().toString();
				if (name.endsWith(".class") && !name.contains("$")) {
					Path relative = testOutDir.relativize(file);
					String className = relative.toString().replace(File.separatorChar, '.');
					className = className.substring(0, className.length() - ".class".length());
					testClasses.add(className);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return testClasses;
	}

	private void compileViaToolApi(CompilerOptions options) throws IOException {
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
		Files.walk(options.srcDir).forEach(new Consumer<Path>() {
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
