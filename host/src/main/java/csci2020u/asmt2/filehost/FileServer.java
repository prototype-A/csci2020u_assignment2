package csci2020u.asmt2.filehost;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class FileServer {

	private ServerSocket serverSocket;
	private File shareDir;
	private CopyOnWriteArrayList<String> fileList;


	public FileServer(int port, String path) throws IOException {

		serverSocket = new ServerSocket(port);

		try {
			// Check that the specified directory path can be accessed
			shareDir = new File(path);
		} catch (Exception e) {
			// Default directory to share is "share/" in the application's dir
			shareDir = new File("./share");
		}
	}

	/**
	 * Start sharing files
	 */
	public void hostFiles() throws IOException {

		System.out.println("Loading file list..");

		buildFileList(shareDir);

		System.out.println("Listening for requests...");

		while (true) {
			Socket clientSocket = serverSocket.accept();
			Thread connThread = new Thread(new ClientConnectionHandler(clientSocket));
			connThread.start();
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

		fileList = new CopyOnWriteArrayList<>();

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
			// Launch file-sharing server
			FileServer fileServer = new FileServer(port, hostShareDir);
			fileServer.hostFiles();
		} catch (IOException e) {
			System.err.println("An I/O Error occurred: " + e.getMessage());
			e.printStackTrace();
		}

	}

}
