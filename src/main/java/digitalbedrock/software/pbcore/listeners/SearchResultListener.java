package digitalbedrock.software.pbcore.listeners;

import java.util.List;

import digitalbedrock.software.pbcore.lucene.HitDocument;

public interface SearchResultListener {

    void searchResultSelected(List<HitDocument> hitDocuments);
}
