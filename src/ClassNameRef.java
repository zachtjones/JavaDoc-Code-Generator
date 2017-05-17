import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClassNameRef {
	/**name of class and documentation at top (fully qualified) */
	private String name;

	/** the url of the java docs for this class */
	private String webURL;

	/** The start of the class description, (ex. Class String) */
	private static String classNameStart = "<h2 title=\"";
	/** The start of the class's interfaces it implements */
	private static String classInterfacesStart = "<dt>All Implemented Interfaces:</dt>\n<dd>";

	public ClassNameRef(String name, String webURL){
		this.name = name;
		this.webURL = webURL;
	}

	/**
	 * Gets the name (fully qualified java name)
	 * @return The name of the class (example java.lang.String)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the full URL of the source java documentation
	 * @return The full URL for the java documentation.
	 */
	public String getWebURL() {
		return webURL;
	}
	
	/**
	 * Gets the file name that would represent this class name reference in the documentation directory.
	 * @param docDirectory The string that is the documentation directory.
	 * @return A string t
	 */
	public String getLocalPath(String docDirectory){
		return docDirectory + '/' + name.replace('.', '/') + ".html";
	}
	
	/**
	 * Gets if the documentation file exists.
	 * @return true iff the documentation file exists locally in the directory.
	 * @param docDirectory The string that is the documentation directory.
	 */
	public boolean docFileExists(String docDirectory){
		return new File(getLocalPath(docDirectory)).exists();
	}
	
	/**
	 * Parses the documentation file into the source code.
	 * Make sure the docFileExists before calling this method.
	 * @param docDirectory The string that is the documentation directory.
	 * @throws IOException If the file can't be read for any reason.
	 */
	public void parseDocs(String docDirectory) throws IOException{
		//fail if there's a bug that this gets called when it shouldn't
		assert docFileExists(docDirectory);
		
		byte[] contentsb = Files.readAllBytes(Paths.get(getLocalPath(docDirectory)));
		String contents = new String(contentsb);
		
		//process the contents
		//the first thing is the name
		int start = contents.indexOf(classNameStart) + classNameStart.length();
		int end = contents.indexOf('\"', start + 1);
		if(start != -1 && end > start){
			//should be of the form "Class ClassName", or "Interface InterfaceName"
			System.out.println(contents.substring(start, end));
		}
		//System.exit(0);
	}
}
