import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class URLDownloader {

	/**
	 * Downloads the contents of the URL Specified.
	 * @param url The URL to download the contents of 
	 * @return The contents, or the empty string ("") if the file can't be downloaded.
	 */
	public static String downloadURL(String url){
		
		try {
			URL website = new URL(url);
			Scanner sc = new Scanner(website.openStream());
			StringBuilder sb = new StringBuilder();
			while(sc.hasNextLine()){
				sb.append(sc.nextLine() + "\n");
			}
			sc.close();
			return sb.toString();
		} catch (IOException e){
			return "";
		}
	}
}
