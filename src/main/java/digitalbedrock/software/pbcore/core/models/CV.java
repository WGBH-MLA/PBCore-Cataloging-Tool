package digitalbedrock.software.pbcore.core.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CV extends CVBase {

    private boolean isAttribute;
    private boolean hasSubs;
    private String bestPractice = "";

    private HashMap<String, CVBase> subs = new HashMap<>();

    public boolean isAttribute() {

        return isAttribute;
    }

    public void setAttribute(boolean isAttribute) {

        this.isAttribute = isAttribute;
    }

    public boolean isHasSubs() {

        return hasSubs;
    }

    public void setHasSubs(boolean hasSubs) {

        this.hasSubs = hasSubs;
    }

    public String getBestPractice() {

        return bestPractice;
    }

    public void setBestPractice(String bestPractice) {

        this.bestPractice = bestPractice;
    }

    public HashMap<String, CVBase> getSubs() {

        return subs;
    }

    public void setSubs(HashMap<String, CVBase> subs) {

        this.subs = subs;
    }

    public CV() {

        super();
    }

    public void update(CV value) {

        addCustomTermsToTermsList(value.getTerms(), getTerms());
        if (!value.isCustom()) {
            return;
        }
        Set<Map.Entry<String, CVBase>> subCVsToProcess = value.getSubs().entrySet();
        // process sub cvs coming from import
        subCVsToProcess.forEach(this::processSubCVEntry);
    }

    private void processSubCVEntry(Map.Entry<String, CVBase> entry) {

        HashMap<String, CVBase> subCVsList = getSubs();

        CVBase subCV = subCVsList.getOrDefault(entry.getKey(), entry.getValue());

        // add custom terms from imported sub cv
        addCustomTermsToTermsList(entry.getValue().getTerms(), subCV.getTerms());

        // add sub cv entry if map does not have key
        if (!subCVsList.containsKey(entry.getKey())) {
            subCVsList.put(entry.getKey(), entry.getValue());
        }
    }

    private void addCustomTermsToTermsList(List<CVTerm> termsToProcess, List<CVTerm> subTermsList) {

        termsToProcess
                .stream()
                .filter(CVTerm::isCustom)
                .filter(Predicate.not(subTermsList::contains))
                .forEach(subTermsList::add);
    }
}
