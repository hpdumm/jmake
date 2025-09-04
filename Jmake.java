import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Optional;

/**
 * Simple build script for Java
 * <p>
 * To use:
 * <ul>
 * <li>Copy this file into your top level project
 * <li>Verify all source code is under dirctory named "src"
 * <li>Create a directory named "lib"
 * <li>Add all jar dependencies to the lib folder
 * <li>From the top level project folder, run {@code java Jmake.java} or
 * {@code java Jmake.java package.MainClass}
 * <ul>
 * <p>
 * 
 * @author dummh
 *
 */
public class Jmake {

	/**
	 * Main method
	 * <p>
	 * To create a library, run in the top level project directory:
	 * <p>
	 * {@code java Jmake.java}
	 * <p>
	 * To create an executable, provide the main class as an argument:
	 * <p>
	 * {@code java Jmake.java package.MainClass}
	 * 
	 * @param args Main class
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		String separator = "--------------------------------------------------------------";

		// get the start time
		long start = System.nanoTime();

		// check that lib path exists
		Path libPath = Paths.get("lib");
		if (!Files.exists(libPath)) {
			throw new IllegalStateException("Directory not found: lib/. At top level of project?");
		}

		// check that src path exists
		Path srcPath = Paths.get("src/main/java");
		if (!Files.exists(srcPath)) {
			// try alternate src path (non maven)
			srcPath = Paths.get("src");
			if (!Files.exists(srcPath)) {
				throw new IllegalStateException("Directory not found: src/. At top level of project?");
			}
		}

		// get the project name (same name as current directory where launched)
		String cwdString = System.getProperty("user.dir");
		String name = Paths.get(cwdString).getFileName().toString();
		Pattern pattern = Pattern.compile("[^-_A-Za-z0-9]");
		Matcher m = pattern.matcher(name);
		if (m.find()) {
			throw new IllegalArgumentException(
					"The name of the project directory must have no spaces and only _, -, A-Z, a-z, 0-9");
		}
		System.out.println(separator);
		System.out.format("Building %s%n", name);
		System.out.println(separator);

		// check for main class
		String mainClass = "";
		if (args.length == 1) {
			System.out.format("Creating executable for main class: %s%n", args[0]);
			mainClass = args[0];
		} else {
			System.out.println("Creating library (no main class provided)");
		}

		// present list of available main classes if not provided?

		// get all the java files and write to a temp file
		Path listfile = Files.createTempFile(null, null);
		try (Stream<Path> files = Files.find(srcPath, 999,
				(path, attribute) -> attribute.isRegularFile() && path.getFileName().toString().endsWith(".java"))) {
			List<String> filenames = files.map(Path::toString).toList();
			Files.write(listfile, filenames);
		}

		// delete the bin folder
		if (Files.isDirectory(Paths.get("bin/"), LinkOption.NOFOLLOW_LINKS)) {
			System.out.println("Deleting bin/ directory");
			Files.walk(Paths.get("./bin")).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}

		// get the os
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
		System.out.format("Compiling on %s with:%n", System.getProperty("os.name"));

		// create the output directory
		boolean created = new File("bin").mkdir();

		// compile with java 17 by default
		String buildCommand = String.format("javac -encoding utf-8 --release 17 -d bin -cp lib/* @%s", listfile);
		Process process;
		if (isWindows) {
			buildCommand = buildCommand.replace("/", "\\");
			buildCommand = buildCommand.replace(":", ";");
		}
		System.out.println("  " + buildCommand);
		process = Runtime.getRuntime().exec(buildCommand.split("\s"));
		InputStream in = process.getErrorStream();
		System.err.println(new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n")));

		// copy the resources into the jar
		Path resourceDir = Paths.get("src/main/resources");
		Path destinationDir = Paths.get("bin/");
		if (Files.exists(resourceDir)) {
			System.out.println("Copying resources");
			Files.walk(resourceDir).forEach(source -> {
				Path destination = Paths.get(destinationDir.toString(),
						source.toString().substring(resourceDir.toString().length()));
				try {
					// System.out.format("destination: %s%n", destination.toString());
					Files.copy(source, destination);
				} catch (java.nio.file.FileAlreadyExistsException e) {
					// ignoring this message (finds that bin already exists)
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}

		// get the current date (maybe one day replace with semver computed from
		// sources)
		LocalDate date = LocalDate.now();
		String dateText = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		// create the jar file (-C changes the working directory to bin/)
		System.out.println("Creating jar file");
		String jarCommand = String.format("jar cf bin/%s-%s.jar -C bin .", name, dateText);
		System.out.println("  " + jarCommand);
		Runtime.getRuntime().exec(jarCommand.split(" "));

		// create executable scripts
		if (!mainClass.isEmpty()) {
			System.out.println("Creating run files");
			// mac or linux
			Files.write(Paths.get(String.format("%s", name)),
					String.format("java -cp bin/*:lib/* %s $@", mainClass).getBytes(StandardCharsets.UTF_8));
			// windows
			Files.write(Paths.get(String.format("%s.bat", name)),
					String.format("java -cp \"bin/*;lib/*\" %s %%*", mainClass).getBytes(StandardCharsets.UTF_8));
		}

		// generate the javadoc
		System.out.println("Creating javadoc");
		String javadocCommand = String.format("javadoc --ignore-source-errors -sourcepath %s -d ./javadoc @%s",
				srcPath.toString(), listfile);
		System.out.println("  " + javadocCommand);
		// javadoc will spit out lots of info that will not be shown here
		Runtime.getRuntime().exec(javadocCommand.split(" "));

		// generate source jar
		System.out.println("Creating source jar");
		String sourceJarCommand = String.format("jar cf bin/%s-%s-sources.jar @%s", name, dateText, listfile);
		System.out.println("  " + sourceJarCommand);
		Runtime.getRuntime().exec(sourceJarCommand.split(" "));

		// run any tests (replacing all of junit with a few lines)
		System.out.println("Running tests\n");
		try (Stream<Path> files = Files.find(srcPath, 999,
				(path, attribute) -> attribute.isRegularFile() && path.getFileName().toString().endsWith(".java"))) {
			List<Path> paths = files.toList();
			for (Path p : paths) {
				// get the package name
				String packageLine = Files.lines(p).filter(line -> line.startsWith("package")).findFirst().get();
				String packageName = packageLine.split(" ")[1].replace(";", "");
				// get class name from filename
				String className = p.getFileName().toString().split(".java")[0];
				String fqn = packageName + "." + className;
				String testCommand = String.format("java -ea -cp bin/*:lib/* %s.%s", packageName, className);
				if (!fqn.equals(mainClass)) {
					if (isWindows) {
						testCommand = testCommand.replace(":", ";");
						testCommand = testCommand.replace("/", "\\");
					}
					System.out.println(testCommand);
					// run command
					process = Runtime.getRuntime().exec(testCommand.split("\s"));
					// print any errors
					InputStream in2 = process.getErrorStream();
					String output = new BufferedReader(new InputStreamReader(in2)).lines()
							.collect(Collectors.joining("\n"));
					if (output.startsWith("Error: Main method not found")) {
						System.err.println("no tests found");
					} else {
						System.err.println(output);
					}
					System.out.println("");
				}
			}
		}

		// delete the temp file list
		Files.delete(listfile);

		// finished
		long stop = System.nanoTime();
		System.out.println(separator);
		System.out.format("Total time: %.3f s%n", (stop - start) / 1_000_000_000.0);
		System.out.println(separator);

	}
}
