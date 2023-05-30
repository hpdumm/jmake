package jmake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		Path srcPath = Paths.get("src");
		if (!Files.exists(srcPath)) {
			throw new IllegalStateException("Directory not found: src/. At top level of project?");
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

		// get all the java files and write to a temp file
		Path listfile = Files.createTempFile(null, null);
		try (Stream<Path> files = Files.find(srcPath, 999,
				(path, attribute) -> attribute.isRegularFile() && path.getFileName().toString().endsWith(".java"))) {
			List<String> filenames = files.map(Path::toString).toList();
			Files.write(listfile, filenames);
		}

		// get the os
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
		System.out.format("Compiling on %s with:%n", System.getProperty("os.name"));

		// compile with java 8 by default
		String[] buildCommand = { "javac", "--source", "8", "--target", "8", "-d", "bin", "-cp", "lib/*", "" };
		buildCommand[9] = String.format("@%s", listfile);
		Process process;
		if (isWindows) {
			buildCommand[8] = "lib\\*";
			System.out.println(String.join(" ", buildCommand));
			process = Runtime.getRuntime().exec(buildCommand);
		} else {
			System.out.println(String.join(" ", buildCommand));
			process = Runtime.getRuntime().exec(buildCommand);
		}
		InputStream in = process.getErrorStream();
		System.err.println(new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n")));
		System.out.println("");

		// get the current date
		LocalDate date = LocalDate.now();
		String dateText = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		// create the jar file (-C changes the working directory to bin/)
		System.out.println(separator);
		System.out.println("Creating jar file");
		System.out.println(separator);
		String[] jarCommand = { "jar", "cf", "", "-C", "bin", "." };
		jarCommand[2] = String.format("%s-%s.jar", name, dateText);
		Runtime.getRuntime().exec(jarCommand);

		// create executable scripts
		if (!mainClass.isEmpty()) {
			System.out.println(separator);
			System.out.println("Creating run files");
			System.out.println(separator);
			// mac or linux
			Files.write(Paths.get(String.format("%s", name)),
					String.format("java -cp *:lib/* %s", mainClass).getBytes(StandardCharsets.UTF_8));
			// windows
			Files.write(Paths.get(String.format("%s.bat", name)),
					String.format("java -cp \"*;lib/*\" %s", mainClass).getBytes(StandardCharsets.UTF_8));
		}

		// generate the javadoc
		System.out.println(separator);
		System.out.println("Creating javadoc");
		System.out.println(separator);
		String[] javadocCommand = { "javadoc", "-d", "javadoc/", "" };
		javadocCommand[3] = String.format("@%s", listfile);
		Runtime.getRuntime().exec(javadocCommand);

		// run any tests (replacing all of junit with a few lines)
		System.out.println(separator);
		System.out.println("Running tests");
		System.out.println(separator);
		try (Stream<Path> files = Files.find(srcPath, 999,
				(path, attribute) -> attribute.isRegularFile() && path.getFileName().toString().endsWith(".java"))) {
			List<Path> paths = files.toList();
			for (Path p : paths) {
				List<String> lines = Files.readAllLines(p);
				String firstLine = lines.get(0);
				String packageName = firstLine.split("package")[1].trim();
				packageName = packageName.split(";")[0];
				String className = p.getFileName().toString().split(".java")[0];
				String[] testCommand = { "java", "-ea", "-cp", "*:lib/*", "" };
				testCommand[4] = String.format("%s.%s", packageName, className);
				if (!testCommand[4].equals(mainClass)) {
					if (isWindows) {
						testCommand[3] = "\"*;lib\\*\"";
					}
					System.out.println(String.join(" ", testCommand));
					// run command
					process = Runtime.getRuntime().exec(testCommand);
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
