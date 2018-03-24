package csci2020u.asmt2.filehost;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class FileServer {

	private ServerSocket serverSocket;
	private File shareDir;
	private ConcurrentHashMap<String, File> fileList;


	public FileServer(int port, String path) throws IOException {

		serverSocket = new ServerSocket(port);
		fileList = new ConcurrentHashMap<>();

		try {
			// Check that the specified directory path exists/can be accessed
			shareDir = new File(path);
		} catch (Exception e) {
			// Default directory to share is "share/" in the application's dir
			shareDir = new File("./share/");
			shareDir.mkdir();
		}
	}

	/**
	 * Start sharing files
	 */
	public void hostFiles() throws IOException {

		// Build list of files to share
		System.out.println("Loading file list...");
		buildFileList(shareDir);
		System.out.println("Loaded file list!\n");

		// Start listening for client connections
		System.out.println("Listening for requests...\n");
		while (true) {
			Socket clientSocket = serverSocket.accept();
			Thread connThread = new Thread(new ClientConnectionHandler(clientSocket, fileList));
			connThread.start();
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

	/**
	 * Handle args and launch the file sharing server
	 */
	public static void main(String[] args) {

		// Default parameters
		int port = 8080;
		String hostShareDir = null;

		// Specified port number
		if (args.length > 0) {
			
			port = Integer.parseInt(args[0]);

			// Specific port and sharing location specified
			if (args.length > 1) {
				hostShareDir = args[1];
			}
		}

		try {
			// Launch file-sharing server and start sharing files
			System.out.println("Launching File Host...\n");
			FileServer fileServer = new FileServer(port, hostShareDir);
			fileServer.hostFiles();
		} catch (IOException e) {
			System.err.println("An I/O Error occurred: " + e.getMessage());
			e.printStackTrace();
		}

	}

}
