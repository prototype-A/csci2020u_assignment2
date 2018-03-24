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
	private BufferedReader requestInput;
	private BufferedInputStream fileInput;
	private BufferedOutputStream fileOutput;
	private ObjectOutputStream responseSender;
	private final int FILE_BUFFER_SIZE = 4096;


	public ClientConnectionHandler(Socket socket, ConcurrentHashMap<String, File> fileList) throws IOException {
		clientSocket = socket;
		this.fileList = fileList;
		requestInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		fileInput = new BufferedInputStream(socket.getInputStream());
		fileOutput = new BufferedOutputStream(socket.getOutputStream());
		responseSender = new ObjectOutputStream(socket.getOutputStream());
	}

	@Override
	public void run() {

		System.out.println("Date: " + (new Date()));
		System.out.println("Established connection with a client from " + clientSocket.getInetAddress().getCanonicalHostName() + ".\n");

		try {
			// Handle the request sent by the client
			parseRequest(requestInput.readLine());

			// Close the connection to the client after handling the request
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				// Maybe it will close now?
				clientSocket.close();
			} catch (IOException e) {
				System.err.println("aaaaaaaaaaaaaaa");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Handle the requests received by the server that is sent from a client
	 *
	 *
	 * @param request The entire command line to parse
	 *
	 * @throws IOException if the command is missing arguments
	 */
	private void parseRequest(String request) {

		try {
			StringTokenizer tokenizer = new StringTokenizer(request);
			String command = tokenizer.nextToken().toUpperCase();

			System.out.println("Date: " + (new Date()));
			System.out.println("Recieved " + command + " command.\n");

			switch (command) {
				case "DIR":			sendFileList(true);
									break;
				case "UPLOAD":		getFile(tokenizer.nextToken(), true);
									break;
				case "DOWNLOAD":	sendFile(tokenizer.nextToken(), true);
									break;
				default:			sendResponse("ERROR", "'" + command + "' method is invalid or unsupported.");
			}

		} catch (NoSuchElementException e) {
			System.out.println("No file name provided");
		}
	}

	/**
	 * Sends the current list of files to the client
	 *
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
			responseSender.writeObject(fileList.keySet());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Download the specified file from client to host
	 * and share it
	 *
	 *
	 * @param fileName The name of the file to download
	 * @param sendOk True - Send acknowledgement of command
	 * False - Do not send acknowledgement
 	 */
	private void getFile(String fileName, boolean sendOk) {

		try {

			// Check if host can create a and write to new file
			File newFile = new File("./share/" + fileName);

			if (sendOk) {
				// Send request confirmation
				sendAcknowledgement();
			}

			// Obtain and write data to file
			FileOutputStream fileOut = new FileOutputStream(newFile);
			byte[] fileByteBuffer = new byte[FILE_BUFFER_SIZE];
			int count;
			while ((count = fileInput.read(fileByteBuffer)) > 0) {
				fileOut.write(fileByteBuffer, 0, count);
				if (count < FILE_BUFFER_SIZE) {
					break;
				}
			}
			System.out.println("Downloaded file to: " + newFile.getCanonicalPath());

			// Add the new file to the file list
			fileList.putIfAbsent(newFile.getName(), newFile);

			// Send updated file list to client
			sendFileList(false);

			// Close the file writer
			fileOut.close();
		} catch (IOException e) {
			sendResponse("ERROR", "Could not write to file");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Sends the requested file over the socket to the client
	 *
	 *
	 * @param fileName The name of the requested file to send
	 * @param sendOk True - Send acknowledgement of command
	 * False - Do not send acknowledgement
	 */
	private void sendFile(String fileName, boolean sendOk) {

		try {

			// Check if file exists on server
			if (fileList.get(fileName) == null) {
				throw new IOException("File not found");
			}

			InputStream fileIn = new FileInputStream(fileList.get(fileName));

			if (sendOk) {
				// Send request confirmation
				sendAcknowledgement();
			}

			// Buffered file output in 4k buffers
			byte[] fileByteBuffer = new byte[4096];
			int count;
			while ((count = fileIn.read(fileByteBuffer)) > 0) {
				fileOutput.write(fileByteBuffer, 0, count);
			}

			fileOutput.flush();
		} catch (Exception e) {
			// Something went wrong: send error
			sendResponse("ERROR", e.getMessage());
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
	 *
	 * @param message The message to send
	 * @param description The description of the message
	 *
	 * @throws IOException if an error occurs while sending the response
	 */
	private void sendResponse(String message, String description) {
		try {
			responseSender.writeObject(new ServerResponse(message, description));
			responseSender.flush();
		} catch (IOException e) {
			System.out.println("Failed to send response to client");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
