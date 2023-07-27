package digitalbedrock.software.pbcore.core;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import digitalbedrock.software.pbcore.core.models.FolderModel;
import digitalbedrock.software.pbcore.utils.Language;
import lombok.Getter;
import lombok.Setter;

public final class Settings implements Serializable {

    private static final long serialVersionUID = 7426471155622776147L;
    private static final int MAX_SAVED_SEARCHES = 10;

    private transient Set<Observer> observers = null;

    private final List<FolderModel> folders = new ArrayList<>();

    private final List<String> savedSearches = new ArrayList<>();
    private boolean firstTimeInstructionsShown = false;
    @Setter
    @Getter
    private Language language;

    public Settings() {

        this.language = Arrays
                .stream(Language.values())
                .filter(lng -> lng.getLocale() == Locale.getDefault())
                .findFirst()
                .orElse(Language.EN);
    }

    public void addObserver(Observer oberserver) {

        if (observers == null) {
            observers = new HashSet<>();
        }
        observers.add(oberserver);
    }

    public void deleteObserver(Observer o) {

        observers.remove(o);
    }

    private void notifyObservers() {

        observers.forEach((obs) -> obs.update(null, this));
    }

    public List<FolderModel> getFolders() {

        return folders;
    }

    public void addFolder(File p) {

        folders.add(new FolderModel(p.getAbsolutePath()));
        notifyObservers();
    }

    public void removePath(String p) {

        FolderModel folderModel1 = folders
                .stream()
                .filter(folderModel -> Objects.equals(folderModel.getFolderPath(), p))
                .findFirst()
                .orElse(null);
        folders.remove(folderModel1);
        notifyObservers();
    }

    public void addSearch(String s) {

        savedSearches.remove(s);
        savedSearches.add(0, s);
        while (savedSearches.size() > MAX_SAVED_SEARCHES) {
            savedSearches.remove(savedSearches.size() - 1);
        }
    }

    public void updateFolder(FolderModel model) {

        FolderModel folderModel1 = folders
                .stream()
                .filter(folderModel -> Objects.equals(folderModel.getFolderPath(), model.getFolderPath()))
                .findFirst()
                .orElse(null);
        if (folderModel1 != null) {
            folderModel1.setIndexing(model.isIndexing());
            folderModel1.setDateLastIndexing(model.getDateLastIndexing());
            folderModel1.setTotalValidFiles(model.getTotalValidFiles());
            folderModel1.setScheduled(model.isScheduled());
        }
        notifyObservers();
    }

    public boolean isFileInFolder(String file) {

        return folders
                .stream()
                .filter(folderModel -> file.contains(folderModel.getFolderPath()))
                .findFirst()
                .orElse(null) != null;
    }

    public boolean isFirstTimeInstructionsShown() {

        return firstTimeInstructionsShown;
    }

    public void markFirstTimeInstructionsShown() {

        firstTimeInstructionsShown = true;
    }
}
