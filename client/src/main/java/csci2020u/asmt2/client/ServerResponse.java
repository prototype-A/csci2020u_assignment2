package csci2020u.asmt2.filehost;

import java.io.Serializable;


public class ServerResponse implements Serializable {

	private String message;
	private String description;


	public ServerResponse(String message, String description) {
		this.message = message;
		this.description = description;
	}

	public String getMessage() {
		return message;
	}

	public String getDesc() {
		return description;
	}

	public String toString() {
		return message + ": " + description;
	}
}
