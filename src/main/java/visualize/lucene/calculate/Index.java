package visualize.lucene.calculate;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import visualize.lucene.Inverted;

import java.io.IOException;
import java.util.logging.Logger;

public class Index {
    private static final Logger logger = Logger.getLogger(String.valueOf(Inverted.class));
    enum VisualModes {
        CONSOLE,
        WEB
    }

    public Index(String[] fields, IndexSearcher searcher, ScoreDoc[] hits) {
        this(searcher, hits, VisualModes.CONSOLE);
    }

    public Index(String field, IndexSearcher searcher, ScoreDoc[] hits, VisualModes mode) {
        visualize(field, searcher, hits, mode);
    }

    public void visualize(String fields, IndexSearcher searcher, ScoreDoc[] hits, VisualModes mode) {
        switch(mode) {
            case CONSOLE:
                printToConsole(fields, searcher, hits);
                break;
            case WEB:
                logger.info("the web option is currently under develop");
                break;
        }
    }

    private void printToConsole(String fields, IndexSearcher searcher, ScoreDoc[] hits) {
        String format = String.format("Found %d hits.", hits.length);
        System.out.println(format);
        for (int i = 0; i < hits.length; ++i) {
            try {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                String msg = String.format("%d. %s\t%s", i + 1, d.get("isbn"), d.get(fields));
                System.out.println(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
