import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class JavaDocGetter {
	
	/**
	 * Reads the java docs for the class, and writes them. 
	 * When this method returns, the documentation HTML file has been generated.
	 * @param c The class name and its url as a ClassNameRef
	 * @param destinationFolder the destination folder for the code
	 */
	public static void generateJavaDocs(ClassNameRef c, String destinationFolder){
		try {
			String filename = c.getLocalPath(destinationFolder);
			File f = new File(filename);
			//skip already existing files
			if(f.exists()){ return; }
			//download and save
			String pageContents = URLDownloader.downloadURL(c.getWebURL());
			f.getParentFile().mkdirs(); //make the directories up to this one
			PrintWriter out = new PrintWriter(filename);
			out.write(pageContents);
			out.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
}
