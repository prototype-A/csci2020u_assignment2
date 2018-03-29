package csci2020u.asmt2.filehost;

import java.io.File;
import java.io.Serializable;
import java.util.Date;


public class FileInfo implements Serializable {

	private String fileName;
	private long fileSize;
	private String readableFileSize;
	private long lastModified;


	public FileInfo(File file) {
		fileName = file.getName();
		fileSize = file.length();
		readableFileSize = getReadableFileSize(fileSize);
		lastModified = file.lastModified();
	}

	public String getFileName() {
		return fileName;
	}

	public String getFileSize() {
		return String.valueOf(fileSize);
	}

	public String getReadableFileSize() {
		return readableFileSize;
	}

	public static String getReadableFileSize(long bytes) {
		// Find largest representation of size
		int unit = 1000;
		if (bytes < unit) {
			return bytes + " B";
		}
		int exp = (int)(Math.log(bytes) / Math.log(unit));
		char pre = ("KMGTPE").charAt(exp-1);
		return String.format("%.1f %cB", bytes / Math.pow(unit, exp), pre);
	}

	public Date getLastModified() {
		return new Date(lastModified);
	}

}
