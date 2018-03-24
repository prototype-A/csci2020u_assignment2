package csci2020u.asmt2.client;

import csci2020u.asmt2.filehost.ServerResponse;
import csci2020u.asmt2.filehost.FileInfo;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.stage.DirectoryChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;


public class Controller {

 	@FXML private TableView<String> clientFileListTable;
	@FXML private TableView<String> serverFileListTable;
	@FXML private TableColumn<String, String> clientFileNameCol;
	@FXML private TableColumn<String, String> serverFileNameCol;
	@FXML private Button downloadButton;
	@FXML private Button uploadButton;
	
	private int port = 8080;
	private File clientShareDir;
	private ObservableList<String> fileList;
	private ObservableList<String> serverFileList;
	private Socket socket;
	private ObjectInputStream responseInput;
	private PrintWriter requestOutput;


	public void initialize() {

		// Prompt user to locate folder to share the contents of
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setInitialDirectory(new File("."));
		dirChooser.setTitle("Select directory to share its contents");
		clientShareDir = dirChooser.showDialog(null);

		try {

			// Build the list of files to share
			fileList = FXCollections.observableArrayList();
			buildFileList(clientShareDir);
			// Display local shared files
			clientFileListTable.setItems(fileList);
			clientFileNameCol.setCellValueFactory(fileName -> new SimpleStringProperty(fileName.getValue()));

			// Connect to file-sharing host
			socket = new Socket(InetAddress.getLocalHost(), port);
			// Set up I/O to the file host
			responseInput = new ObjectInputStream(socket.getInputStream());
			requestOutput = new PrintWriter(socket.getOutputStream());

			// Request list of shared files from server
			sendRequest("DIR");

			// Close everything
			responseInput.close();
			requestOutput.close();
			socket.close();
		} catch (ConnectException e) {
			System.out.println("Connection refused.");
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
	 * Send a request to the file server
	 *
	 * @param command The command to send
	 * @exception IOException if an I/O error occurs while reading the server response
	 */
	private void sendRequest(String command) throws IOException {

		String request = command + "\r\n";

		requestOutput.print(request);
		requestOutput.flush();

		System.out.println("Sent Request: " + request);

		// Get the response from the file server after sending the request
		getResponse();

		switch (command) {
			case "DIR":			receiveFileList();
								break;
			case "UPLOAD":		
								break;
			case "DOWNLOAD":	
								break;
		}
	}

	private void receiveFileList() {
		try {
			serverFileList = FXCollections.observableArrayList((Set<String>)responseInput.readObject());
			serverFileListTable.setItems(serverFileList);
			serverFileNameCol.setCellValueFactory(fileName -> new SimpleStringProperty(fileName.getValue()));
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Failed to receive file list from server");
		}
	}

	/**
	 * Send a request that involves a file to the file server
	 *
	 * @param command The command to send
	 * @param fileName The name of the file involved
	 *
	 * @exception IOException if an I/O error occurs while reading the server response
	 */
	private void sendRequest(String command, String fileName) throws IOException {
		sendRequest(command + " " + fileName + "\r\n");
	}

	/**
	 * Read and print the response sent by the server
	 *
	 * @exception IOException if an I/O error occurs while reading the response
	 */
	private void getResponse() throws IOException {

		try {
			ServerResponse response = (ServerResponse)responseInput.readObject();

			System.out.print("Response from server: " + response.toString() + "\n");
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found");
			e.printStackTrace();
		}
	}

	/**
	 * Called when the "Download" button is clicked
	 */
	public void downloadFile(ActionEvent event) {
		String selectedFileName = serverFileListTable.getSelectionModel().getSelectedItem();
		if (selectedFileName != null) {
			System.out.println(selectedFileName);
		} else {
			System.out.println("No file selected to download");
		}
	}

	/**
	 * Called when the "Upload" button is clicked
	 */
	public void uploadFile(ActionEvent event) {
		String selectedFileName = clientFileListTable.getSelectionModel().getSelectedItem();
		if (selectedFileName != null) {
			System.out.println(selectedFileName);
		} else {
			System.out.println("No file selected to upload");
		}
	}

}
