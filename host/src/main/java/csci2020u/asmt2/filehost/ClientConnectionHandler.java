package csci2020u.asmt2.filehost;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


public class ClientConnectionHandler implements Runnable {

	private Socket clientSocket;
	private ConcurrentHashMap<String, File> fileList;
	private ConcurrentHashMap<String, FileInfo> fileInfoList;
	private BufferedReader requestInput;
	private ObjectOutputStream responseSender;
	private final int FILE_BUFFER_SIZE = 4096;


	public ClientConnectionHandler(Socket socket, ConcurrentHashMap<String, File> fileList, ConcurrentHashMap<String, FileInfo> fileInfoList) throws IOException {
		clientSocket = socket;
		this.fileList = fileList;
		this.fileInfoList = fileInfoList;
		requestInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		responseSender = new ObjectOutputStream(socket.getOutputStream());
	}

	@Override
	public void run() {

		// Client connected
		logMessage("Established connection with a client");

		try {
			// Handle the request sent by the client
			parseRequest(requestInput.readLine());

			// Close the connection to the client after handling the request
			clientSocket.close();
		} catch (IOException e) {
			logError("Could not close connection to client");
		} finally {
			try {
				// Maybe it will close now?
				clientSocket.close();
			} catch (IOException e) {
				logError("Connection not closed", e);
			}
		}
	}

	/**
	 * Handle the requests received by the server 
	 * that is sent from a client
	 *
	 * @param request The entire command line to parse
	 */
	private void parseRequest(String request) {

		try {
			StringTokenizer tokenizer = new StringTokenizer(request);
			String command = tokenizer.nextToken().toUpperCase();

			logMessage("Recieved " + command + " command from client");

			switch (command) {
				case "DIR":			sendFileList(true);
									break;
				case "DOWNLOAD":	sendFile(tokenizer.nextToken(), true);
									break;
				case "UPLOAD":		receiveFile(tokenizer.nextToken(), true);
									break;
				default:			sendResponse("ERROR", "'" + command + "' method is invalid or unsupported.");
			}

		} catch (NoSuchElementException e) {
			logError("No file name provided");
		}
	}

	/**
	 * Sends the current list of files to the client
	 *
	 * @param sendOk True - Send acknowledgement of command
	 * False - Do not send acknowledgement
	 */
	private void sendFileList(boolean sendOk) {
		
		try {

			if (sendOk) {
				// Send request confirmation
				sendAcknowledgement();
			}

			// Send the file list
			responseSender.writeObject(fileInfoList);
		} catch (Exception e) {
			logError(e.getMessage(), e);
		}
	}

	/**
	 * Sends the requested file over the socket to the client
	 *
	 * @param fileName The name of the requested file to send
	 * @param sendOk True - Send acknowledgement of command
	 * False - Do not send acknowledgement
	 */
	private void sendFile(String fileName, boolean sendOk) {

		try {

			// Check if file exists on server
			if (!fileList.get(fileName).exists()) {
				throw new IOException("File not found");
			}

			// Open file and buffered stream to output file bytes
			InputStream fileIn = new FileInputStream(fileList.get(fileName));
			BufferedOutputStream fileOutput = new BufferedOutputStream(clientSocket.getOutputStream());

			if (sendOk) {
				// Send request confirmation
				sendAcknowledgement();
			}

			// Buffered file output
			byte[] fileByteBuffer = new byte[FILE_BUFFER_SIZE];
			int count;
			while ((count = fileIn.read(fileByteBuffer)) > 0) {
				fileOutput.write(fileByteBuffer, 0, count);
			}

			fileOutput.flush();
			fileIn.close();
			fileOutput.close();
		} catch (IOException e) {
			logError(e.getMessage());
		} catch (Exception e) {
			logError(e.getMessage(), e);
		}
	}

	/**
	 * Download the specified file from client to host
	 * and share it
	 *
	 * @param fileName The name of the file to download
	 * @param sendOk True - Send acknowledgement of command
	 * False - Do not send acknowledgement
 	 */
	private void receiveFile(String fileName, boolean sendOk) {

		try {

			// Check if the host can create a and write to new file
			File newFile = new File("./share/" + fileName);

			if (sendOk) {
				// Send request confirmation
				sendAcknowledgement();
			}

			// Open a buffered stream to receive file byte buffers
			BufferedInputStream fileInput = new BufferedInputStream(clientSocket.getInputStream());

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

			logMessage("Received " + fileName + " from client");

			// Add the new file to the file list if it doesn't exist already
			fileList.putIfAbsent(newFile.getName(), newFile);
			fileInfoList.putIfAbsent(newFile.getName(), new FileInfo(newFile));

			// Send updated file list to client
			sendFileList(false);

			// Close the file writer
			fileOut.close();
		} catch (IOException e) {
			logError("Could not write to file");
		} catch (Exception e) {
			logError(e.getMessage(), e);
		}
	}

	/**
	 * Sends a confirmation message from the server to the client
	 */
	private void sendAcknowledgement() {
		sendResponse("OK", "Acknowledged");
	}

	/**
	 * Sends a response message from the server to the client
	 *
	 * @param message The message to send
	 * @param description The description of the message
	 */
	private void sendResponse(String message, String description) {
		try {
			responseSender.writeObject(new ServerResponse(message, description));
			responseSender.flush();
		} catch (IOException e) {
			logError("Failed to send response to client");
		} catch (Exception e) {
			logError(e.getMessage(), e);
		}
	}

	/**
	 * Output the log message to the command line with the timestamp 
	 * and client's IP address
	 *
	 * @param message The message to log
	 */
	private void logMessage(String message) {
		message = "[" + new Date() + "]\n" + message + " at ";
		System.out.println(message + clientSocket.getInetAddress().toString() + "\n");
	}

	/**
	 * Output the error message to the command line with the timestamp 
	 * and a red highlighted error indicator
	 *
	 * @param message The error message to log
	 */
	public static void logError(String message) {
		message = "[" + new Date() + "]\n\033[1;31mERROR:\033[1;0m " + message + "\n";
		System.err.println(message);
	}

	/**
	 * Output the error message to the command line with the timestamp 
	 * and a red highlighted error indicator, and print the function
	 * call stack
	 *
	 * @param message The error message to log
	 * @param exception The exception thrown
	 */
	public static void logError(String message, Exception exception) {
		logError(message);
		exception.printStackTrace();
	}

}
