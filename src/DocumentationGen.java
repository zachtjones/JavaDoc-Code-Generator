import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class DocumentationGen extends Application {

	private TextArea t;
	private Label lblDownload;
	private Label lblParse;
	
	private String outputDestination;
	
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		AnchorPane ap = new AnchorPane();
		ap.setPrefSize(800, 400);
		
		lblDownload = new Label("");
		lblDownload.setLayoutX(10);
		lblDownload.setLayoutY(35);
		lblDownload.setPrefSize(400, 30);
		ap.getChildren().add(lblDownload);
		
		lblParse = new Label("");
		lblParse.setLayoutX(415);
		lblParse.setLayoutY(35);
		lblParse.setPrefSize(380, 30);
		ap.getChildren().add(lblParse);
		
		Button b = new Button("Generate c headers/sources from java documentation online.");
		b.setLayoutX(10);
		b.setLayoutY(10);
		b.setPrefSize(780, 20);
		b.setOnAction(event -> {
			FileChooser fd = new FileChooser();
			fd.setTitle("Choose your destination folder");
			fd.getExtensionFilters().add(new ExtensionFilter("Any (*.*)", "*.*"));
			File opened = fd.showSaveDialog(primaryStage);
			if (opened == null){
				return;
			}
			this.outputDestination = opened.getAbsolutePath();
			t.appendText("Destination folder: " + this.outputDestination + "\nGetting class list:\n");
			this.getClasses();
		});
		ap.getChildren().add(b);

		t = new TextArea();
		t.setLayoutX(10);
		t.setLayoutY(80);
		t.setPrefSize(780, 325);
		ap.getChildren().add(t);
		
		primaryStage.setResizable(false);
		
		primaryStage.setScene(new Scene(ap));
		primaryStage.setTitle("Java -> C Compiler");
		primaryStage.show();
		
	}
	
	/**
	 * Gets the classes in java (all of them)
	 */
	private void getClasses(){
		Thread thread = new Thread(() -> {
			ClassNameRef[] classes = JavaClassGetter.getClassList();
			if (classes == null){
				Platform.runLater(() -> {
					t.appendText("Error - could not get class info\n");
				});
				return;
			}
			//start the documentation parsing stuff.
			this.parseDocs(classes);
			//download all
			for (int i = 0; i < classes.length; i++){
				final int temp = i; //need final variable in enclosing scope
				Platform.runLater(() -> {
					t.appendText("Downloading class: '" + classes[temp].getName() + "'\n");
					lblDownload.setText("Downloading class " + temp + " of " + classes.length);
				});
				JavaDocGetter.generateJavaDocs(classes[i], this.outputDestination);
			}
		});
		thread.setDaemon(true);
		thread.setName("Class loading thread");
		thread.start();
		
	}
	
	private void parseDocs(ClassNameRef[] classes){
		Thread thread = new Thread(() -> {
			//convert to linked list for quick re-ordering
			LinkedList<ClassNameRef> l = new LinkedList<ClassNameRef>();
			for(ClassNameRef i : classes){
				l.add(i);
			}
			//parse the documentation for each if it is ready, else move to back
			while(l.size() != 0){
				ClassNameRef r = l.removeFirst();
				//it is ready
				if(r.docFileExists(this.outputDestination)){
					try {
						int numOn = classes.length - l.size(); //the number working on
						Platform.runLater(() -> {
							t.appendText("Parsing class: '" + r.getName() + "'\n");
							lblParse.setText("Parsing class " + numOn + " of " + classes.length);
						});
						r.parseDocs(this.outputDestination);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					//move to back end of 'queue'
					l.add(r);
					//Yield to prevent infinite loops from killing processor if waiting for download
					Thread.yield();
				}
			}
		});
		thread.setDaemon(true);
		thread.setName("Doc parsing thread");
		thread.start();
	}

}
