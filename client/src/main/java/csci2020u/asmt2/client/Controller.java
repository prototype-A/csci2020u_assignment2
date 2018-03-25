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
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
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
	@FXML private TextArea fileInfoArea;
	
	private int port = 8080;
	private File clientShareDir;
	private HashMap<String, File> fileList;
	private ObservableList<String> serverFileList;
	private Socket socket;
	private ObjectInputStream responseInput;
	private DataOutputStream requestOutput;
	private final int FILE_BUFFER_SIZE = 4096;


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
			updateClientFileList();
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
	 * @param fileDir The directory to begin in
	 */
	private void buildFileList(File fileDir) {

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

	private void updateClientFileList() {
		// Display local shared files
		clientFileListTable.setItems(FXCollections.observableArrayList(fileList.keySet()));
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
			System.err.println("Could not connect to file host");
		} catch (UnknownHostException e) {
			System.err.println("Unknown connection");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a file-related request to the file host
	 *
	 *
	 * @param command The command to send
	 *
	 * @throws IOException if an I/O error occurs while sending the request
	 * or reading the response from the host
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

		// Get the response from the file host after sending the request
		getResponse();

		switch (command) {
			case "DIR":			receiveFileList();
								break;
			case "DOWNLOAD":	receiveFile(fileName);
								break;
			case "UPLOAD":		sendFile(fileName);
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
			System.err.println("Class not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Failed to receive file list from host");
		}
	}

	/**
	 * Downloads the specified file from the file host
	 *
	 *
	 * @param fileName The name of the file to download
	 *
	 * @throws IOException if an I/O error occurs while downloading file
	 */
	private void receiveFile(String fileName) throws IOException {

		// Check if the client can create a and write to new file
		File newFile = new File(clientShareDir.getAbsolutePath() + "/" + fileName);

		// Open a buffered stream to receive file byte buffers
		BufferedInputStream fileInput = new BufferedInputStream(socket.getInputStream());

		// Download and write data to file
		FileOutputStream fileOut = new FileOutputStream(newFile);
		byte[] fileByteBuffer = new byte[FILE_BUFFER_SIZE];
		int count;
		while ((count = fileInput.read(fileByteBuffer)) > 0) {
			fileOut.write(fileByteBuffer, 0, count);

			// Stop after writing the last buffer containing file contents
			if (count < FILE_BUFFER_SIZE) {
				break;
			}
		}
		System.out.println("File downloaded\n");

		// Add the new file to the file list
		fileList.put(newFile.getName(), newFile);
		updateClientFileList();

		// Close the file writer
		fileOut.close();
	}

	/**
	 * Uploads the specified file to the host
	 *
	 *
	 * @param fileName The name of the file to upload
	 *
	 * @throws IOException if an I/O error occurs while reading the response
	 */
	private void sendFile(String fileName) throws IOException {

		// Check if file exists on client
		if (!fileList.get(fileName).exists()) {
			throw new IOException("File not found");
		}

		InputStream fileIn = new FileInputStream(fileList.get(fileName));

		// Buffered file output
		byte[] fileByteBuffer = new byte[FILE_BUFFER_SIZE];
		int count;
		while ((count = fileIn.read(fileByteBuffer)) > 0) {
			requestOutput.write(fileByteBuffer, 0, count);
		}
		System.out.println("File uploaded\n");

		// Receive updated file list from host
		receiveFileList();

		// Clean-up
		requestOutput.flush();
		requestOutput.close();
		fileIn.close();
	}

	/**
	 * Read and print the response sent by the file host
	 *
	 * @throws IOException if an I/O error occurs while reading the response
	 */
	private void getResponse() throws IOException {

		try {
			ServerResponse response = (ServerResponse)responseInput.readObject();

			System.out.println("Response from host: (\"" + response.toString() + "\")\n");
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
			e.printStackTrace();
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
				System.err.println("Failed to download file from host");
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
				System.err.println("Failed to upload file to host");
			}
		} else {
			System.out.println("No file selected to upload");
		}
	}

}
