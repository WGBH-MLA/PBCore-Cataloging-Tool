package digitalbedrock.software.pbcore.controllers.settings;

import static digitalbedrock.software.pbcore.controllers.SettingsCrawlingController.*;
import static digitalbedrock.software.pbcore.utils.I18nKey.*;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import digitalbedrock.software.pbcore.MainApp;
import digitalbedrock.software.pbcore.core.models.FolderModel;
import digitalbedrock.software.pbcore.lucene.LuceneIndexer;
import digitalbedrock.software.pbcore.utils.LanguageManager;

public class FolderStateCellFactory
        implements Callback<TableColumn<FolderModel, String>, TableCell<FolderModel, String>> {

    @Override
    public TableCell<FolderModel, String> call(final TableColumn<FolderModel, String> param) {

        return new TableCell<FolderModel, String>() {

            @Override
            public void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                }
                else {
                    FolderModel model = MainApp.getInstance().getRegistry().getSettings().getFolders().get(getIndex());
                    LanguageManager languageManager = LanguageManager.INSTANCE;
                    model.scheduledProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
                        boolean processingFolder = LuceneIndexer
                                .getInstance()
                                .isProcessingFolder(model.getFolderPath());
                        if (Boolean.TRUE.equals(newValue)) {
                            setText(languageManager.getString(SCHEDULED));
                            return;
                        }
                        if (processingFolder) {
                            setText(languageManager.getString(PROCESSING));
                            return;
                        }
                        setText(languageManager.getString(FINISHED));
                    }));
                    model.indexingProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
                        boolean scheduledFolder = LuceneIndexer.getInstance().isScheduledFolder(model.getFolderPath());
                        if (Boolean.TRUE.equals(newValue)) {
                            setText(languageManager.getString(PROCESSING));
                            return;
                        }
                        if (scheduledFolder) {
                            setText(languageManager.getString(SCHEDULED));
                            return;
                        }
                        setText(languageManager.getString(FINISHED));
                    }));
                    LuceneIndexer
                            .getInstance()
                            .getFoldersToProcess()
                            .addListener((ListChangeListener<String>) c -> Platform
                                    .runLater(() -> Platform.runLater(() -> {
                                        if (c.getList().contains(model.getFolderPath())) {
                                            setText(languageManager.getString(SCHEDULED));
                                        }
                                    })));
                    if (model.isIndexing()) {
                        setText(languageManager.getString(PROCESSING));
                    }
                    else if (model.isScheduled()) {
                        setText(languageManager.getString(SCHEDULED));
                    }
                    else {
                        setText(languageManager.getString(FINISHED));
                    }
                }
            }
        };
    }
}
