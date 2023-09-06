package digitalbedrock.software.pbcore.controllers;

import static digitalbedrock.software.pbcore.utils.I18nKey.*;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import digitalbedrock.software.pbcore.MainApp;
import digitalbedrock.software.pbcore.controllers.settings.*;
import digitalbedrock.software.pbcore.core.models.FolderModel;
import digitalbedrock.software.pbcore.lucene.LuceneIndexer;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.LanguageManager;

public class SettingsCrawlingController extends AbsController {

    @FXML
    private TableView<FolderModel> foldersTableView;

    @FXML
    private TableColumn<FolderModel, String> pathColumn;

    @FXML
    private TableColumn<FolderModel, String> lastIndexedColumn;

    @FXML
    private TableColumn<FolderModel, String> stateColumn;

    @FXML
    private TableColumn<FolderModel, Long> filesProcessedColumn;

    @FXML
    private TableColumn<FolderModel, Boolean> removeColumn;

    @FXML
    private TableColumn<FolderModel, Boolean> reindexColumn;

    @FXML
    private Button addButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button okButton;

    @FXML
    private AnchorPane apFirstTime;

    @FXML
    void onAddButtonClick(ActionEvent event) {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File folder = directoryChooser.showDialog(addButton.getScene().getWindow());
        if (folder == null) {
            return;
        }
        if (MainApp
                .getInstance()
                .getRegistry()
                .getSettings()
                .getFolders()
                .stream()
                .anyMatch(fm -> fm.getFolderPath().contains(folder.getAbsolutePath()))) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.INVALID_FOLDER));
            alert.setHeaderText(null);
            alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.INVALID_FOLDER_DESCRIPTION));
            alert.showAndWait();
            return;
        }
        MainApp.getInstance().getRegistry().getSettings().addFolder(folder);
        FolderModel folderModel = new FolderModel(folder.getPath());
        LuceneIndexer instance = LuceneIndexer.getInstance();
        if (instance.startFolderIndexing(folderModel.getFolderPath())) {
            folderModel.setScheduled(true);
        }
        MainApp.getInstance().getRegistry().getSettings().updateFolder(folderModel);
        foldersTableView.getItems().add(folderModel);

    }

    @FXML
    void onCancelButtonClick(ActionEvent event) {

        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @FXML
    void onOkButtonClick(ActionEvent event) {

        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        pathColumn.setCellValueFactory(celldata -> new SimpleStringProperty(celldata.getValue().getFolderPath()));
        stateColumn.setCellValueFactory(celldata -> {
            if (celldata.getValue().isIndexing()) {
                return new SimpleStringProperty(LanguageManager.INSTANCE.getString(PROCESSING));
            }
            else if (celldata.getValue().isScheduled()) {
                return new SimpleStringProperty(LanguageManager.INSTANCE.getString(SCHEDULED));
            }
            else {
                return new SimpleStringProperty(LanguageManager.INSTANCE.getString(FINISHED));
            }
        });
        lastIndexedColumn
                .setCellValueFactory(celldata -> new SimpleStringProperty(
                        celldata.getValue().getDateLastIndexing() == null ? ""
                                : celldata
                                        .getValue()
                                        .getDateLastIndexing()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        filesProcessedColumn
                .setCellValueFactory(celldata -> new SimpleLongProperty(celldata.getValue().getTotalValidFiles())
                        .asObject());

        ObservableList<FolderModel> obsList = FXCollections
                .observableArrayList(MainApp.getInstance().getRegistry().getSettings().getFolders());
        foldersTableView.setItems(obsList);

        lastIndexedColumn.setCellFactory(new FolderLastIndexedDateCellFactory());
        stateColumn.setCellFactory(new FolderStateCellFactory());
        filesProcessedColumn.setCellFactory(new FolderProcessedFilesCellFactory());
        reindexColumn.setCellFactory(new ReindexFolderCellFactory());
        removeColumn.setCellFactory(new RemoveFolderCellFactory(obsList));

        boolean instructionsShown = MainApp.getInstance().getRegistry().getSettings().isFirstTimeInstructionsShown();
        apFirstTime.setVisible(!instructionsShown);
        if (!instructionsShown) {
            MainApp.getInstance().getRegistry().markFirstTimeInstructionsShown();
        }
    }

    @Override
    public MenuBar createMenu() {

        return new MenuBar();
    }

    public void onDismissFirstTimeInstructions(ActionEvent actionEvent) {

        apFirstTime.setVisible(false);
    }
}
