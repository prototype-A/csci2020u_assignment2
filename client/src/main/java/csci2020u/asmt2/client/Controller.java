package csci2020u.asmt2.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;


public class Controller {

 	@FXML private TableView<String> clientFileList;
	@FXML private TableView<String> serverFileList;
	@FXML private Button downloadButton;
	@FXML private Button uploadButton;


	public void initialize() {
		
	}

	/**
	 * Called when the "Download" button is clicked
	 */
	public void downloadFile(ActionEvent event) {
		String selectedFileName = serverFileList.getSelectionModel().getSelectedItem();
		if (selectedFileName != null) {
			System.out.println(selectedFileName);
		} else {
			System.out.println("No file selected to download");
		}
	}

	/**
	 * Called when the "Upload" button is clicked
	 */
	public void uploadFile(ActionEvent event) {
		String selectedFileName = clientFileList.getSelectionModel().getSelectedItem();
		if (selectedFileName != null) {
			System.out.println(selectedFileName);
		} else {
			System.out.println("No file selected to upload");
		}
	}

}
