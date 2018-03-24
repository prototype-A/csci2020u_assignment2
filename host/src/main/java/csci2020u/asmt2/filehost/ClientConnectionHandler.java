package csci2020u.asmt2.filehost;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
	private DataOutputStream responseOutput;
	private ObjectOutputStream objectSender;


	public ClientConnectionHandler(Socket socket, ConcurrentHashMap<String, File> fileList) throws IOException {
		clientSocket = socket;
		this.fileList = fileList;
		requestInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		responseOutput = new DataOutputStream(socket.getOutputStream());
		objectSender = new ObjectOutputStream(socket.getOutputStream());
	}

	@Override
	public void run() {

		System.out.println("Date: " + (new Date()));
		System.out.println("Established connection with a client from" + clientSocket.getInetAddress().getCanonicalHostName() + ".\n");

		try {
			// Handle the request sent by the client
			parseRequest(requestInput.readLine());

			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				requestInput.close();
				responseOutput.close();
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
	 * @exception IOException if the command is missing arguments
	 */
	private void parseRequest(String request) {

		try {
			StringTokenizer tokenizer = new StringTokenizer(request);
			String command = tokenizer.nextToken().toUpperCase();

			System.out.println("Date: " + (new Date()));
			System.out.println("Recieved " + command + " command.\n");

			switch (command) {
				case "DIR":			sendFileList();
									break;
				case "UPLOAD":		getFile(tokenizer.nextToken());
									break;
				case "DOWNLOAD":	sendFile(tokenizer.nextToken());
									break;
				default:			//sendResponse("ERROR", "'" + command + "' method is invalid or unsupported.");
			}

		} catch (NoSuchElementException e) {
			System.out.println("No file name provided");
		}
	}

	/**
	 * Sends the current list of files to the client
	 */
	private void sendFileList() {
		
		try {
			// Send request confirmation
			sendAcknowledgement();

			// Send the file list
			objectSender.writeObject(fileList.keySet());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getFile(String fileName) {

		try {

			// Check if host can create a and write to new file
			File newFile = new File(fileName);

			// Send request confirmation
			sendAcknowledgement();

			ObjectInputStream objectIn = new ObjectInputStream(clientSocket.getInputStream());
			File downloadedFile = (File)objectIn.readObject();
			System.out.println("Downloaded file to: " + downloadedFile.getCanonicalPath());

		} catch (IOException e) {
			sendResponse("ERROR", "Could not write to file");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Sends the requested file over the socket
	 *
	 *
	 * @param fileName The name of the requested file to send
	 */
	private void sendFile(String fileName) {

		try {

			// Check if file exists on server
			if (fileList.get(fileName) == null) {
				throw new IOException("File not found");
			}

			InputStream fileIn = new FileInputStream(fileList.get(fileName));

			// Send request confirmation
			sendAcknowledgement();

			// Buffered file byte output
			byte[] fileByteBuffer = new byte[4096];
			int count;
			while ((count = fileIn.read(fileByteBuffer)) > 0) {
				responseOutput.write(fileByteBuffer, 0, count);
			}

			responseOutput.flush();
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
	 * @exception IOException if an error occurs while sending the response
	 */
	private void sendResponse(String message, String description) {
		try {
			objectSender.writeObject(new ServerResponse(message, description));
			objectSender.flush();
		} catch (IOException e) {
			System.out.println("Failed to send response to client");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
