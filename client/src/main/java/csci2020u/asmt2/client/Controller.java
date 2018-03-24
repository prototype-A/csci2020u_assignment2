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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
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
	private HashMap<String, File> fileList;
	private ObservableList<String> serverFileList;
	private Socket socket;
	private ObjectInputStream responseInput;
	private ObjectOutputStream fileOutput;
	private DataOutputStream requestOutput;


	public void initialize() {

		// Prompt user to locate folder to share the contents of
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setInitialDirectory(new File("."));
		dirChooser.setTitle("Select directory to share its contents");
		clientShareDir = dirChooser.showDialog(null);

		try {

			// Build the list of files to share
			fileList = new HashMap<>();
			buildFileList(clientShareDir);
			// Display local shared files
			clientFileListTable.setItems(FXCollections.observableArrayList(fileList.keySet()));
			clientFileNameCol.setCellValueFactory(fileName -> new SimpleStringProperty(fileName.getValue()));

			// Connect to file-sharing host
			connectToHost();

			// Request list of shared files from host
			sendRequest("DIR", null);

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
	 * @throws IOException if an I/O error occurs while reading a file
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
					fileList.put(file.getName(), file);
				}
			}
		}
	}

	/**
	 * Establish connection and data stream I/O to the file-sharing host 
	 */
	private void connectToHost() {

		try {
			// Connect to file-sharing host
			socket = new Socket(InetAddress.getLocalHost(), port);
			// Set up I/O to the file host
			responseInput = new ObjectInputStream(socket.getInputStream());
			requestOutput = new DataOutputStream(socket.getOutputStream());
		} catch (ConnectException e) {
			System.out.println("Connection refused");
		} catch (UnknownHostException e) {
			System.out.println("Unknown connection");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a file-related request to the file server
	 *
	 * @param command The command to send
	 * @throws IOException if an I/O error occurs while reading the server response
	 */
	private void sendRequest(String command, String fileName) throws IOException {

		// Construct request to send
		String request = command;
		if (fileName != null) {
			request += " " + fileName;
		}

		// Send the request to the host
		requestOutput.writeBytes(request + "\r\n");
		requestOutput.flush();

		System.out.println("Sent Request: " + request);

		// Get the response from the file server after sending the request
		getResponse();

		switch (command) {
			case "DIR":			receiveFileList();
								break;
			case "UPLOAD":		sendFile(fileName);
								break;
			case "DOWNLOAD":	//receiveFile(fileName);
								break;
		}
	}

	/**
	 * Obtain the (updated) list of shared files from the host and
	 * display it on the client
	 */
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
	 * Uploads the specified file to the host
	 *
	 *
	 * @param fileName The name of the file to upload
	 *
	 * @throws IOException if an I/O error occurs while reading the response
	 */
	private void sendFile(String fileName) {

		try {

			// Check if file exists on client
			if (fileList.get(fileName) == null) {
				throw new IOException("File not found");
			}

			InputStream fileIn = new FileInputStream(fileList.get(fileName));

			// Buffered file output in 4k buffers
			byte[] fileByteBuffer = new byte[4096];
			int count;
			while ((count = fileIn.read(fileByteBuffer)) > 0) {
				requestOutput.write(fileByteBuffer, 0, count);
			}

			// Receive updated file list from host
			receiveFileList();

			// Clean-up
			requestOutput.flush();
			requestOutput.close();
			fileIn.close();
		} catch (IOException e) {
			
		}
	}

	/**
	 * Called when the "Download" button is clicked
	 */
	public void downloadFile(ActionEvent event) {
		String selectedFile = serverFileListTable.getSelectionModel().getSelectedItem();
		if (selectedFile != null) {
			try {
				// Connect to host
				connectToHost();

				// Request to download a file from the host
				System.out.println("Downloading " + selectedFile + " from host");
				sendRequest("DOWNLOAD", selectedFile);
			} catch (IOException e) {
				System.out.println("Failed to download file from host");
			}
		} else {
			System.out.println("No file selected to download");
		}
	}

	/**
	 * Called when the "Upload" button is clicked
	 */
	public void uploadFile(ActionEvent event) {
		String selectedFile = clientFileListTable.getSelectionModel().getSelectedItem();
		if (selectedFile != null) {
			try {
				// Connect to host
				connectToHost();

				// Request to upload a file to the host
				System.out.println("Sending " + selectedFile + " to host");
				sendRequest("UPLOAD", selectedFile);
			} catch (IOException e) {
				System.out.println("Failed to upload file to host");
			}
		} else {
			System.out.println("No file selected to upload");
		}
	}

	/**
	 * Read and print the response sent by the server
	 *
	 * @throws IOException if an I/O error occurs while reading the response
	 */
	private void getResponse() throws IOException {

		try {
			ServerResponse response = (ServerResponse)responseInput.readObject();

			System.out.println("Response from server: " + response.toString() + "\n");
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found");
		}
	}

}
