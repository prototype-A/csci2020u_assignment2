package csci2020u.asmt2.filehost;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


public class ClientConnectionHandler implements Runnable {

	private Socket clientSocket;
	private BufferedReader requestInput;
	private DataOutputStream responseOutput;


	public ClientConnectionHandler(Socket socket) throws IOException {
		clientSocket = socket;
		requestInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		responseOutput = new DataOutputStream(socket.getOutputStream());
	}

	@Override
	public void run() {
		System.out.println("Established connection with a client");

		String firstLine;

		try {
			firstLine = requestInput.readLine();
			handleRequest(firstLine);
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

	public void handleRequest(String request) throws IOException {
		try {
			StringTokenizer tokenizer = new StringTokenizer(request);
			String command = tokenizer.nextToken();
			String uri = tokenizer.nextToken();

			if (command.equalsIgnoreCase("GET") || command.equalsIgnoreCase("POST")) {
				System.out.println("Recieved " + command + " command");
				sendResponse(200, "OK", "Acknowledged");
			} else {
				sendResponse(405, "Method Not Allowed", "You cannot use the '" + command + "' command on this server");
			}
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		}
	}

	private void sendResponse(int code, String message, String description) throws IOException {
		/*
		HTTP/1.1 200 OK
		<headers>
		<html>
		...
		</html>
		*/
		String responseCode = "HTTP/1.1 " + code + " " + message + "\r\n";
		String headers =	"Content-Type: text/html\r\n" + 
							"Date: " + (new Date()) + "\r\n";
		String content =	"<!DOCTYPE html>" + 
							"<html>" + 
							"	<head>" + 
							"		<title>" + code + ": " + message + "</title>" + 
							"	</head>" + 
							"	<body>" + 
							"		<h1>" + code + ": " + message + "</h1>" + 
							"		<p>" + description + "</p>" + 
							"	</body>" + 
							"</html>";

		responseOutput.writeBytes(responseCode);
		responseOutput.writeBytes("Content-Type: text/html\r\n");
		responseOutput.writeBytes("Date: " + (new Date()) + "\r\n");
		responseOutput.writeBytes("Server: Simple-Http-Server v1.0.0\r\n");
		responseOutput.writeBytes("Content-Length: " + content.length() + "\r\n");
		responseOutput.writeBytes("Connection: Close\r\n\r\n");

		responseOutput.write(content.getBytes());
		responseOutput.flush();
	}

}
