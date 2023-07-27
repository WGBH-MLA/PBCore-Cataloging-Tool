package digitalbedrock.software.pbcore.lucene;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

import org.apache.lucene.index.IndexWriter;
import org.xml.sax.SAXException;

import digitalbedrock.software.pbcore.core.PBcoreValidator;

class LuceneIndexingTask extends Task<Boolean> {

    public static final Logger LOGGER = Logger.getLogger(LuceneIndexingTask.class.getName());
    private final IndexWriter indexWriter;
    private final String folder;
    private final String file;

    public LuceneIndexingTask(IndexWriter indexWriter, String folder, String file) {

        this.folder = folder;
        this.file = file;
        this.indexWriter = indexWriter;
    }

    public String getFolder() {

        return folder;
    }

    public String getFile() {

        return file;
    }

    @Override
    protected Boolean call() {

        if (isCancelled()) {
            return null;
        }
        try {
            File f = new File(file);
            new PBcoreValidator().validate(f);
            LuceneEngine.createOrUpdateIndexesForFile(indexWriter, f, folder);
            indexWriter.commit();
            return true;
        }
        catch (IOException | SAXException e) {
            LOGGER.log(Level.WARNING, "error while creating or updating indexes for file", e);
            return false;
        }
    }
}
