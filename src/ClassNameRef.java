import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClassNameRef {
	/**name of class (fully qualified; ex: java.lang.String) */
	private String name;

	/** the url of the java docs for this class */
	private String webURL;

	private static String copyrightInfo = "///@author Zach Jones\n";

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
	 * Gets the short name.
	 * @return The name of the class (example "String" for java.lang.String)
	 */
	public String getShortName(){
		if(name.lastIndexOf('.') != -1){
			return name.substring(name.lastIndexOf('.') + 1);
		} else {
			return name;
		}
	}

	/**
	 * Gets the full URL of the source java documentation
	 * @return The full URL for the java documentation.
	 */
	public String getWebURL() {
		return webURL;
	}

	/**
	 * Gets the file name that would represent this class name reference 
	 * in the documentation directory.
	 * @param docDirectory The string that is the documentation directory.
	 * @return A string to read the HTML copied from javadocs.
	 */
	public String getLocalPath(String docDirectory){
		return docDirectory + '/' + name.replace('.', '/') + ".html";
	}
	/**
	 * Gets the file name that would represent this class name reference 
	 * in the documentation directory.
	 * @param docDirectory The string that is the documentation directory.
	 * @return A string that is the filename for the java generated source.
	 */
	public String getLocalSource(String docDirectory){
		return docDirectory + '/' + name.replace('.', '/') + ".java";
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

		//process the text
		//the writers for the file
		PrintWriter sourcePW = new PrintWriter(this.getLocalSource(docDirectory));
		// @author stuff
		sourcePW.println(copyrightInfo);
		sourcePW.print("package ");
		sourcePW.print(this.name.replace("." + this.getShortName(), ""));
		sourcePW.println(';');

		//print the access modifier(s), the extends and implements as fully qualified names
		int start = contents.indexOf("<pre>");
		int end = contents.indexOf("</pre>");
		if (start < 0 || end < 0) {
			sourcePW.close();
			throw new IOException("No class found for: " + this.getName());
		}
		start += 5;
		String declaration = contents.substring(start, end);
		declaration = declaration.replace("<span class=\"typeNameLabel\">", ""); 
		declaration = declaration.replace("</span>", "");
		declaration = declaration.replace("\n", " ").replace("</a>", "").replace("../", "");
		declaration = declaration.replaceAll(" title=\".*?\">", " ");
		declaration = declaration.replace("<a href=\"", "").replace('/', '.');
		declaration = declaration.replace("&lt;", "<").replace("&gt;", ">");
		declaration = declaration.replaceAll("\\.\\w*\\.html\" ", ".");
		sourcePW.println(declaration + " {\n");

		int fieldsConsFuncsStart = contents.indexOf("<h3>Field Detail</h3>");
		for (int currIndex = contents.indexOf("<h4>", fieldsConsFuncsStart); 
				currIndex > -1; 
				currIndex = contents.indexOf("<h4>", currIndex + 1)) {

			// process the field/constructor/method
			if (contents.indexOf("</div>", currIndex) == -1) {
				continue;
			}
			String total = contents.substring(currIndex, contents.indexOf("</div>", currIndex));
			if (!total.contains("<pre>")) {
				continue;
			}
			String tempDec = total.substring(total.indexOf("<pre>") + 5, total.indexOf("</pre>"));
			tempDec = tempDec.replace("&nbsp;", " ").replace("../", "");
			tempDec = tempDec.replaceAll("title=\".*?\">\\w*</a>", "");
			tempDec = tempDec.replace("<a href=\"", "").replace(".html\"", "").replace('/', '.');
			tempDec = tempDec.replace("&lt;", "<").replace("&gt;", ">");
			tempDec = tempDec.replaceAll("\\s+", " ");//replace multiple white-spaces out
			if (tempDec.contains("Deprecated")) {
				continue; //skip methods, constructors, and fields that are deprecated.
			}
			// the declaration is now done, parse the text to show in the comments 
			//  (useful for implementation details)
			if (total.indexOf("<div") == -1 ) {
				System.out.println("Error: " + this.getName() + ": docs invalid for: " + tempDec);
				continue;
			}
			String tempDocs = total.substring(total.indexOf("<div"));
			tempDocs = tempDocs.replaceAll("<.*?>", "");
			// make tempDocs into the doc strings
			tempDocs = "\t/** " + tempDocs.replace("\n", "\n\t*") + " */";

			sourcePW.println(tempDocs);
			sourcePW.print('\t' + tempDec);
			if (tempDec.contains("(") && !tempDec.contains("abstract")) {
				// method or constructor, has {}
				sourcePW.println("{\n\t\tTODO\n\t}");
			} else {
				sourcePW.println(';');
			}
			sourcePW.println();
		}

		sourcePW.println('}');

		sourcePW.flush();
		sourcePW.close();
	}
}