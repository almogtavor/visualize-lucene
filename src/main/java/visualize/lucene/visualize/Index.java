package visualize.lucene.visualize;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import visualize.lucene.VisualizeLucene;

import java.io.IOException;
import java.util.logging.Logger;

public class Index {
    private static final Logger logger = Logger.getLogger(String.valueOf(VisualizeLucene.class));
    private String field;
    private IndexSearcher searcher;
    private ScoreDoc[] hits;
    private VisualModes mode;
    enum VisualModes {
        CONSOLE,
        WEB
    }

    public Index(String field, IndexSearcher searcher, ScoreDoc[] hits) {
        this(field, searcher, hits, VisualModes.CONSOLE);
    }

    public Index(String field, IndexSearcher searcher, ScoreDoc[] hits, VisualModes mode) {
        this.field = field;
        this.searcher = searcher;
        this.hits = hits;
        this.mode = mode;
    }

    public void visualize() {
        switch(mode) {
            case CONSOLE:
                printToConsole(field, searcher, hits);
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
                // TODO: read with read instead of searcher.doc
                Document d = searcher.doc(docId);
                String msg = String.format("%d. %s\t%s", i + 1, d.get("isbn"), d.get(fields));
                System.out.println(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
