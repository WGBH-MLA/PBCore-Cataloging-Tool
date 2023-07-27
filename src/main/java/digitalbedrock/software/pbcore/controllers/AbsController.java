package digitalbedrock.software.pbcore.controllers;

import javafx.fxml.Initializable;
import javafx.scene.control.MenuBar;

import digitalbedrock.software.pbcore.listeners.MenuListener;
import lombok.Setter;

public abstract class AbsController implements Initializable {

    @Setter
    protected MenuListener menuListener;

    public abstract MenuBar createMenu();

    public void onShown() {

    }
}
