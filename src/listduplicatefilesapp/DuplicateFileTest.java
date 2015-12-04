package listduplicatefilesapp;

import static java.nio.file.FileVisitResult.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class DuplicateFileTest {

	// map to link file names to a map of file attributes
	public static Map<String, Map<Path, BasicFileAttributes>> m = new HashMap<>();
	
	// method to populate the map
	public static void mapFile(String filename, Path path, BasicFileAttributes attr) {
		Map<Path, BasicFileAttributes> mattr = m.get(filename);
		if (mattr == null) {
			m.put(filename, mattr=new HashMap<Path, BasicFileAttributes>());
		}
		mattr.put(path, attr);
	}
	// method to write output to a temporary CSV file
	public static void appendToCSV(Path outputFile, String s) {
		Charset charset = Charset.forName(System.getProperty("file.encoding"));
		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, charset, StandardOpenOption.APPEND)) {
			writer.write(s, 0, s.length());
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}
	}
	// nested static class to define file tree walking behavior
	public static class ListFiles extends SimpleFileVisitor<Path> {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
			if (attr.isSymbolicLink()) {
				return CONTINUE;
			} else {
				mapFile(file.getFileName().toString(), file, attr);
				return CONTINUE;
			}
		}
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			System.err.println(exc);
			return CONTINUE;
		}
	}
	// create temporary CSV and write output to it
	public static void outputMap(Map<String, Map<Path, BasicFileAttributes>> m,
									  String header, boolean writeToCSV) {
		if (writeToCSV) {
			// create temporary file in which to write output
			try {
				Path tempFile = Files.createTempFile(null, ".csv");
				System.out.format("The temporary file" +
					" has been created: %s%n", tempFile);
				
				// add header to CSV
				appendToCSV(tempFile, header);
				
				// iterate through map to populate CSV
				// for keys whose value, which is a map,
				// contain more than one value
				for (Map<Path, BasicFileAttributes> mattr : m.values()) {
					if (mattr.size() > 1) {
						for (Map.Entry<Path, BasicFileAttributes> e : mattr.entrySet()) {
							String fs = String.format(
								"%n%s, %s, %s, %s",
								e.getKey().getFileName(),
								e.getKey().getParent(),
								e.getValue().creationTime(),
								e.getValue().size()
							);
							appendToCSV(tempFile, fs);
						}
					}
				}
			} catch (IOException x) {
				System.err.format("IOException: %s%n", x);
			}
		} else {
			// print header
			System.out.println(header);
			
			// iterate through map to populate CSV
			// for keys whose value, which is a map,
			// contain more than one value
			for (Map<Path, BasicFileAttributes> mattr : m.values()) {
				if (mattr.size() > 1) {
					for (Map.Entry<Path, BasicFileAttributes> e : mattr.entrySet()) {
						String fs = String.format(
							"%s, %s, %s, %s",
							e.getKey().getFileName(),
							e.getKey().getParent(),
							e.getValue().creationTime(),
							e.getValue().size()
						);
						System.out.println(fs);
					}
				}
			}
		}
	}
	public static void main(String[] args) throws IOException {
		Path startingDir = Paths.get(System.getProperty("user.home"));
		ListFiles lf = new ListFiles();
		Files.walkFileTree(startingDir, lf);
		
		String header = "FileName, Path, DateCreated, FileSize";
		boolean writeToCSV = true;
		
		outputMap(m, header, writeToCSV);
	}
}
