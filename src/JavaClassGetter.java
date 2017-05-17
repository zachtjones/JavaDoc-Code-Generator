import java.util.ArrayList;

public class JavaClassGetter {
	
	private static String baseURL = "https://docs.oracle.com/javase/8/docs/api/";

	/** The file's URL that lists all the classes with links from the java docs */
	private static String fileURL = baseURL + "allclasses-noframe.html";


	public static ClassNameRef[] getClassList(){
		String contents = URLDownloader.downloadURL(fileURL);
		String[] lines = contents.split("\n");
		ArrayList<ClassNameRef> classes = new ArrayList<>();
		for (String i : lines){
			if(i.startsWith("<li><a href=\"")){
				i = i.replace("<li><a href=\"", "");
				int endIndex = i.indexOf("\""); //end double quote of thing
				if(endIndex == -1){ continue; }
				i = i.substring(0, endIndex);
				String name = i.replace('/', '.');
				name = name.replace(".html", "");
				classes.add(new ClassNameRef(name, baseURL + i));
			}
		}
		
		//convert to array
		ClassNameRef[] items = new ClassNameRef[classes.size()];
		for(int i = 0; i < items.length; i++){
			items[i] = classes.get(i);
		}
		return items;
	}
}
