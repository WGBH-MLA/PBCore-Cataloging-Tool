package digitalbedrock.software.pbcore.lucene;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import digitalbedrock.software.pbcore.core.models.entity.IPBCore;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LuceneEngineSearchFilter {

    private final StringProperty term = new SimpleStringProperty();
    private List<IPBCore> fieldsToSearch = new ArrayList<>();
    private boolean allElements;

    public LuceneEngineSearchFilter() {

        this.term.set("");
        this.allElements = true;
    }

    public StringProperty termProperty() {

        return term;
    }

    public String getTerm() {

        return term.get();
    }

    public void setTerm(String term) {

        this.term.set(term);
    }

    public List<IPBCore> getFieldsToSearch() {

        return fieldsToSearch;
    }

    public void setFieldsToSearch(List<IPBCore> fieldsToSearch) {

        this.fieldsToSearch = fieldsToSearch;
    }

    public boolean isAllElements() {

        return allElements;
    }

    public void updateFieldsToSearch(List<IPBCore> collect, int totalElements) {

        if (collect.size() == totalElements) {
            allElements = true;
        }
        else {
            allElements = false;
            fieldsToSearch.clear();
            fieldsToSearch.addAll(collect);
        }
    }

    public long getAttributesCount() {

        return fieldsToSearch.stream().filter(IPBCore::isAttribute).count();
    }

    public long getElementsCount() {

        return fieldsToSearch.stream().filter(ipbCore -> !ipbCore.isAttribute()).count();
    }

    public void setAllElements(boolean allElements) {

        this.allElements = allElements;
    }

    public void setAsAllElementsFilter() {

        allElements = true;
        this.fieldsToSearch.clear();
    }
}
