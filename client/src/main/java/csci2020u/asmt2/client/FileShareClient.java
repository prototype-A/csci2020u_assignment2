package csci2020u.asmt2.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class FileShareClient extends Application {
	
	private final static int WIN_WIDTH = 800;
	private final static int WIN_HEIGHT = 600;


	@Override
	public void start(Stage primaryStage) throws Exception {

		// Load the FXML
		FXMLLoader loader = new FXMLLoader(getClass().getResource("UI.fxml"));
        Parent root = loader.load();

		// Application window
		primaryStage.setTitle("FileShare Client");
		primaryStage.setScene(new Scene(root, WIN_WIDTH, WIN_HEIGHT));
		primaryStage.show();

	}

	/**
	 * Launch the JavaFX application
	 */
	public static void main(String[] args) {
		launch(args);
	}
}
