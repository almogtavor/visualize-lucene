package visualize.lucene.initialize;

import lombok.Getter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import visualize.lucene.VisualizeLucene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import static visualize.lucene.initialize.Utils.addDoc;

/**
 * create index
 */
public class SearcherCreator {
    private static final Logger logger = Logger.getLogger(String.valueOf(VisualizeLucene.class));
    private final Properties props;
    // TODO: Change this to *:* if that'll work
    private static final String defaultQueryString = "lucene";
    private static final String[] defaultQueryFields = {"title"};
    private static final int totalHitsThreshold = 1000;
    private static final int hitsPerPage = 10;
    @Getter
    private IndexSearcher searcher;
    @Getter
    private IndexReader reader;
    @Getter
    private TopDocs topDocs;
    @Getter
    private ScoreDoc[] hits;
    @Getter
    private Query query;

    public SearcherCreator(Properties props) {
        this(props, defaultQueryString);
    }

    public SearcherCreator(Properties props, String queryString) {
        this(props, queryString, defaultQueryFields);
    }

    public SearcherCreator(Properties props, String queryString, String[] defaultQueryFields) {
        this.props = props;
        init(queryString, defaultQueryFields);
    }

    /**
     * Specify the analyzer for tokenizing text.
     * The same analyzer should be used for indexing and searching
     */
    public void init(String queryString, String[] defaultQueryFields) {
        try (StandardAnalyzer analyzer = new StandardAnalyzer()) {
            String pathOfIndex = Objects.requireNonNull(props).getProperty("pathOfIndex");
            createSearcherOfIndex(Path.of(pathOfIndex), analyzer, queryString);
        }
    }

    public void createSearcherOfIndex(Path pathOfIndex, StandardAnalyzer analyzer, String queryString) {
        try (Directory index = new MMapDirectory(pathOfIndex)) {
            writeContent(analyzer, index);
            try {
                query = new MultiFieldQueryParser(defaultQueryFields, analyzer).parse(queryString);
                reader = DirectoryReader.open(index);
                searcher = new IndexSearcher(reader);
                TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, totalHitsThreshold);
                searcher.search(query, collector);
                topDocs = collector.topDocs();
                hits = topDocs.scoreDocs;
            } catch (ParseException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeContent(StandardAnalyzer analyzer, Directory index) throws IOException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(index, indexWriterConfig);
        addDoc(indexWriter, "We hold that proof beyond a reasonable doubt is required.", "193398817");
        addDoc(indexWriter, "Lucene for Dummies", "55320055Z");
        addDoc(indexWriter, "We hold that proof requires reasoanble preponderance of the evidenceb. Lucene is an example", "55063554A");
        addDoc(indexWriter, "The Art of Computer Science", "9900333X");
        indexWriter.close();
    }

    public void closeReader() {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
