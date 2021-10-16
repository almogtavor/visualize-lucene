package visualize.lucene;

import visualize.lucene.initialize.SearcherCreator;
import visualize.lucene.visualize.Index;
import visualize.lucene.visualize.InvertedIndex;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;

public class VisualizeLucene {
    private static final Logger logger = Logger.getLogger(String.valueOf(VisualizeLucene.class));

    public static void main(String[] args) throws IOException {
        Properties props = loadProps();
        SearcherCreator searcherCreator = new SearcherCreator(props);
        Index index = new Index("title",
                searcherCreator.getSearcher(),
                searcherCreator.getHits()
        );
        index.visualize();
        InvertedIndex invertedIndex = new InvertedIndex(
                searcherCreator.getSearcher(),
                searcherCreator.getReader(),
                searcherCreator.getQuery(),
                searcherCreator.getTopDocs()
        );
        invertedIndex.visualize();
        searcherCreator.closeReader();
    }

    private static Properties loadProps() {
        try (InputStream input =
                     new FileInputStream(
                             "src/main/resources/application.properties")) {
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);
            return prop;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (Properties) Collections.emptyMap();
    }
}
