package csci2020u.asmt2.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class FileShareClient extends Application {


	@Override
	public void start(Stage primaryStage) throws Exception {

		// Load the FXML
		FXMLLoader loader = new FXMLLoader(getClass().getResource("UI.fxml"));
        Parent root = loader.load();

		// Send command-line arguments to controller
		ClientController clientController = (ClientController)loader.getController();
		clientController.setConnSettings(getParameters().getRaw());

		// Application window
		primaryStage.setTitle("FileShare Client");
		primaryStage.setScene(new Scene(root, 800, 600));
		primaryStage.show();

	}

	/**
	 * Launch the JavaFX application
	 */
	public static void main(String[] args) {
		launch(args);
	}
}
