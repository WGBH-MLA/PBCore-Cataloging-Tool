package digitalbedrock.software.pbcore.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import digitalbedrock.software.pbcore.core.models.CVTerm;

public class CVTermItemController {

    @FXML
    private Label termLabel;
    @FXML
    private Label sourceLabel;

    public void bind(CVTerm cvTerm) {

        termLabel.setText(cvTerm.getTerm());
        sourceLabel.setText(cvTerm.getSource());
    }
}
