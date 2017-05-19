import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class ClassNameRef {
	/**name of class (fully qualified; ex: java.lang.String) */
	private String name;

	/** the url of the java docs for this class */
	private String webURL;

	/** The start of the class description, (ex. Class String) */
	private static String nameStart = "<h2 title=\"";
	/** The start of the inheritance pattern */
	private static String inheritanceStart = "<ul class=\"inheritance\">\n<li><a href=\"";	
	/** The start of the class's interfaces it implements */
	private static String interfacesStart = "<dt>All Implemented Interfaces:</dt>\n<dd>";
	/** The start of the class's constructor's details */
	private static String constructorStart = "<h3>Constructor Detail</h3>";
	

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
	 * @return A string that is the filename for the c generated source.
	 */
	public String getLocalSource(String docDirectory){
		return docDirectory + '/' + name.replace('.', '/') + ".c";
	}
	/**
	 * Gets the file name that would represent this class name reference 
	 * in the documentation directory.
	 * @param docDirectory The string that is the documentation directory.
	 * @return A string that is the filename for the c generated header.
	 */
	public String getLocalHeader(String docDirectory){
		return docDirectory + '/' + name.replace('.', '/') + ".h";
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
		int start = contents.indexOf(nameStart) + nameStart.length();
		int end = contents.indexOf('\"', start + 1);
		String name;
		if(start != -1 && end > start){
			//should be of the form "Class ClassName", or "Interface InterfaceName", 
			//  or "Enum EnumName"
			name = contents.substring(start, end);
		} else {
			throw new IOException("Name of Class, Interface, or Enum not found.");
		}
		
		if(name.startsWith("Class")){
			decodeClass(docDirectory, contents);
			System.exit(0); //TODO
		} else if(name.startsWith("Interface")){
			
		} else if(name.startsWith("Enum")){
			
		} else {
			throw new IOException("Type: " + name + " is not an class, interface, or enum");
		}
		
		
		
		//name is like 'Class ClassName', superClass is like 'java.lang.Object'
		//System.exit(0);
	}
	
	private void decodeClass(String docDirectory, String contents) throws IOException{
		//the writers for the class
		PrintWriter headerPW = new PrintWriter(this.getLocalHeader(docDirectory));
		PrintWriter sourcePW = new PrintWriter(this.getLocalSource(docDirectory));
		
		//write top info
		//convert java.lang.String to JAVA_LANG_STRING_H (c convention for headers)
		String headerDefine = this.getName().toUpperCase().replace('.', '_') + "_H";
		headerPW.println("#ifdef " + headerDefine);
		headerPW.println("#define " + headerDefine);
		//typedef
		String nameWithUnderscores = this.getName().replace('.', '_');
		headerPW.println("\ntypedef struct " + nameWithUnderscores + "_S * " + nameWithUnderscores);
		
		//determine if it is generic (has the &lt; and &gt; in the name line 
		// name line: (<h2 title="Class ClassName" .. > Class ClassName<T1,T2>))
		int start = contents.indexOf(nameStart);
		int end = contents.indexOf("\n", start);
		String nameLine = contents.substring(start, end);
		start = nameLine.indexOf("&lt;") + 4;
		end = nameLine.indexOf("&gt;", start);
		String[] generics = null;
		if(start != 3){ //has generics
			String gene = nameLine.substring(start, end);
			generics = gene.split(",");
		}
		if(generics != null){
			sourcePW.print("@generic ");
			for (String g : generics){
				sourcePW.print(g + " ");
			}
			sourcePW.println();			
		}

		//for the superClass, need the last one (the direct parent, not its grandparent)
		//this is fully qualified
		String superClass = null;
		start = contents.indexOf(inheritanceStart, start) + inheritanceStart.length();
		end = contents.indexOf('\"', start + 1);
		do {
			superClass = contents.substring(start, end);
			//correct the link to be the class name
			superClass = superClass.replace("../", "").replace('/', '_').replace(".html", "");
			System.out.println("Superclass: " + superClass);
			start = contents.indexOf(inheritanceStart, start) + inheritanceStart.length();
			end = contents.indexOf('\"', start + 1);
		} while(start != -1 + inheritanceStart.length() && end > start);
		//have obtained parent class, or null if there isn't one 
		//(most classes will be java.lang.object)
		if(superClass != null) { 
			sourcePW.println("@extends " + superClass);
		}
		
		//get interfaces implemented
		start = contents.indexOf(interfacesStart, start) + interfacesStart.length();
		end = contents.indexOf("\n", start);
		String[] interfaces = null;
		if(start > interfacesStart.length() && end > start){
			String inter = contents.substring(start, end);
			interfaces = inter.split("<a href=\"");
			for (int i = 0; i < interfaces.length; i++){
				//extract the interfaces in the notation used previously
				interfaces[i] = interfaces[i].substring(0, interfaces[i].indexOf('\"'));
				interfaces[i] = interfaces[i].replace("../", "")
						.replace(".html", "").replace('/', '_');
			}
		}
		if(interfaces != null){
			sourcePW.print("@implements ");
			for(String i : interfaces){
				sourcePW.print(i);
			}
			sourcePW.println();
		}
		System.out.println("Implments: " + Arrays.toString(interfaces));
		
		//include statements that will almost always be used
		sourcePW.println("#include <stdint.h>"); //for any number value (ex. int32_t for int)
		sourcePW.println("#include <stdbool.h>"); //for booleans
		sourcePW.println("#include <stdlib.h>"); //malloc - always used
		
		//constructors
		start = contents.indexOf(constructorStart, end);
		if(start != -1){
			while(start != 4){
				start = contents.indexOf("<pre>", end) + 5;
				end = contents.indexOf("</pre>", start) - 6;
				String def = contents.substring(start, end).replace('\n', ' ');
				System.out.println(def);
			}
		} else {
			//default constructor ClassName ClassName_init();
			headerPW.println("/** Default constructor for " + this.getName() + ". */");
			headerPW.println(nameWithUnderscores + " " + nameWithUnderscores + "_init();");
		}
		
		//use lastIndexOf(String, int) for searching backwards
		
		//print the ending of the header
		headerPW.println("#endif");
		
		headerPW.flush();
		sourcePW.flush();
		headerPW.close();
		sourcePW.close();
	}
}