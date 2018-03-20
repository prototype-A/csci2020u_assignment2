package csci2020u.asmt2.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;


public class FileShareClient extends Application {
	
	private final static int WIN_WIDTH = 800;
	private final static int WIN_HEIGHT = 600;
	private int port = 8080;
	private File clientShareDir;
	private ObservableList<String> fileList;
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	@Override
	public void start(Stage primaryStage) throws Exception {

		// Prompt user to locate folder to share the contents of
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setInitialDirectory(new File("."));
		dirChooser.setTitle("Select directory to share its contents");
		clientShareDir = dirChooser.showDialog(primaryStage);

		// Build the list of files to share
		buildFileList(clientShareDir);

		// Load the FXML
        Parent root = FXMLLoader.load(getClass().getResource("UI.fxml"));

		// Application window
		primaryStage.setTitle("FileShare Client");
		primaryStage.setScene(new Scene(root, WIN_WIDTH, WIN_HEIGHT));
		primaryStage.show();


		try {
			socket = new Socket(InetAddress.getLocalHost(), port);

			// Set up the I/O
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());

			// Send the request
			System.out.println("Request: ");
			System.out.print("GET HTTP/1.1\r\n");
			System.out.print("Host: \r\n\r\n");
			out.print("GET HTTP/1.1\r\n");
			out.print("Host: \r\n\r\n");
			out.flush();

			// Read and print the response
			System.out.println("Response:");
			String line;
			while((line = in.readLine()) != null) {
				System.out.println(line);
			}

			// Close everything
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Recursively iterates through all files and subdirectories
	 * and adds all files to a list
	 *
	 *
	 * @param fileDir The directory to begin in
	 *
	 * @exception IOException if an I/O error occurs while reading a file
	 * /sub-directory
	 */
	private void buildFileList(File fileDir) throws IOException {

		fileList = FXCollections.observableArrayList();

		// Check directory permissions and whether it exists or not
		if (fileDir.exists() && fileDir.canRead()) {

			// Read contents of fileDir
			for (File file: fileDir.listFiles()) {

				if (file.isDirectory()) {
					// Sub-directory: Recurse over it
					buildFileList(file);
				} else {
					// File: Add to list
					fileList.add(file.getName());
				}
			}
		}
	}

	/**
	 * Launch the JavaFX application
	 */
	public static void main(String[] args) {
		launch(args);
	}
}
