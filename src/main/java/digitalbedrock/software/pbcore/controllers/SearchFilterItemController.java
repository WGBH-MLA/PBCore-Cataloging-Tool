package digitalbedrock.software.pbcore.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import digitalbedrock.software.pbcore.lucene.LuceneEngineSearchFilter;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.LanguageManager;

public class SearchFilterItemController {

    @FXML
    private TextField textFieldTerm;
    @FXML
    private Button buttonRemove;
    @FXML
    private Button buttonManage;
    @FXML
    private Label lblElementsCount;
    @FXML
    private Label lblAttributesCount;

    private int index;

    public SearchFilterItemController() {

    }

    public void updateIndex(int i) {

        this.index = i;
    }

    public void bind(int i, LuceneEngineSearchFilter searchFilter,
                     FilterInteractionListener filterInteractionListener) {

        this.index = i;
        buttonRemove.setOnAction(event -> {
            if (filterInteractionListener != null) {
                filterInteractionListener.onRemove(index, searchFilter);
            }
        });
        buttonManage.setOnAction(event -> {
            if (filterInteractionListener != null) {
                filterInteractionListener.onSelectElements(index, searchFilter);
            }
        });
        textFieldTerm.textProperty().addListener((observable, oldValue, newValue) -> searchFilter.setTerm(newValue));
        textFieldTerm.setText(searchFilter.getTerm());
        LanguageManager languageManager = LanguageManager.INSTANCE;
        lblElementsCount
                .setText(searchFilter.isAllElements() ? languageManager.getString(I18nKey.ALL)
                        : (String.valueOf(searchFilter.getElementsCount())));
        lblAttributesCount
                .setText(searchFilter.isAllElements() ? languageManager.getString(I18nKey.ALL)
                        : (String.valueOf(searchFilter.getAttributesCount())));
    }

    public interface FilterInteractionListener {

        void onRemove(int index, LuceneEngineSearchFilter searchFilter);

        void onSelectElements(int index, LuceneEngineSearchFilter searchFilter);
    }
}
