package visualize.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import static visualize.lucene.Method2.printWholeIndex;

public class Inverted {
    private static final Logger logger = Logger.getLogger(String.valueOf(Inverted.class));

    public static void main(String[] args) throws IOException {
        Properties props = loadProps();

        // 0. Specify the analyzer for tokenizing text.
        //    The same analyzer should be used for indexing and searching
        try (StandardAnalyzer analyzer = new StandardAnalyzer()) {
            // 1. create the index
            String pathOfIndex = Objects.requireNonNull(props).getProperty("pathOfIndex");
            String pathOfIndex2 = Objects.requireNonNull(props).getProperty("pathOfIndex2");
            try (Directory index = new MMapDirectory(Paths.get(pathOfIndex))) {
                IndexWriterConfig config = new IndexWriterConfig(analyzer);

                IndexWriter indexWriter = new IndexWriter(index, config);
                addDoc(indexWriter, "We hold that proof beyond a reasonable doubt is required.", "193398817");
                addDoc(indexWriter, "Lucene for Dummies", "55320055Z");
                addDoc(indexWriter, "We hold that proof requires reasoanble preponderance of the evidenceb. Lucene is an example", "55063554A");
                addDoc(indexWriter, "The Art of Computer Science", "9900333X");
                indexWriter.close();

                // 2. query
                String queryString = args.length > 0 ? args[0] : "lucene";

                // the "title" arg specifies the default field to use
                // when no field is explicitly specified in the query.
                Query query = null;
                String title = "title";
                try {
                    String[] fields = {title};
                    query = new MultiFieldQueryParser(fields, analyzer).parse(queryString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                // 3. search
                int hitsPerPage = 10;
                IndexReader reader = DirectoryReader.open(index);
                IndexSearcher searcher = new IndexSearcher(reader);
                TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, 1000);
                searcher.search(query, collector);
                ScoreDoc[] hits = collector.topDocs().scoreDocs;

                // 4. display results
                String format = String.format("Found %d hits.", hits.length);
                System.out.println(format);
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    String msg = String.format("%d. %s\t%s", i + 1, d.get("isbn"), d.get(title));
                    System.out.println(msg);
                }

                // reader can only be closed when there
                // is no need to access the documents any more.
                vis(pathOfIndex2);
//        printWholeIndex(searcher);
                reader.close();
            }
        }
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

    private static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));

        // use a string field for isbn because we don't want it tokenized
        doc.add(new StringField("isbn", isbn, Field.Store.YES));
        w.addDocument(doc);
    }

    public static void vis(String path) {
        try {
            try (Directory directory = new MMapDirectory(Paths.get(path))) {
                Analyzer analyzer = new StandardAnalyzer();
                IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                IndexWriter writer = new IndexWriter(directory, iwc);

                FieldType type = new FieldType();
                type.setStoreTermVectors(true);
                type.setStoreTermVectorPositions(true);
                type.setStoreTermVectorOffsets(true);
                type.setIndexOptions(IndexOptions.DOCS);

                Field fieldStore =
                        new Field("text", "We hold that proof beyond a reasonable doubt is required.", type);
                Document doc = new Document();
                doc.add(fieldStore);
                writer.addDocument(doc);

                fieldStore =
                        new Field(
                                "text",
                                "We hold that proof requires reasoanble preponderance of the evidenceb.",
                                type);
                doc = new Document();
                doc.add(fieldStore);
                writer.addDocument(doc);

                writer.close();

                DirectoryReader reader = DirectoryReader.open(directory);
                IndexSearcher searcher = new IndexSearcher(reader);

                MatchAllDocsQuery query = new MatchAllDocsQuery();
                TopDocs hits = searcher.search(query, Integer.MAX_VALUE);

                BiFunction<Integer, Integer, Set<String>> mergeValue =
                        (docId, pos) -> {
                            TreeSet<String> treeSet = new TreeSet<>();
                            treeSet.add((docId + 1) + ":" + pos);
                            return treeSet;
                        };

                Map<String, Set<String>> invertedIndex = new HashMap<>();
                for (ScoreDoc scoreDoc : hits.scoreDocs) {
                    Fields termVs = reader.getTermVectors(scoreDoc.doc);
                    Terms terms = termVs.terms("text");
                    TermsEnum termsIt = terms.iterator();
                    PostingsEnum docsAndPosEnum = null;
                    BytesRef bytesRef;
                    while ((bytesRef = termsIt.next()) != null) {
                        docsAndPosEnum = termsIt.postings(docsAndPosEnum, PostingsEnum.ALL);
                        docsAndPosEnum.nextDoc();
                        int pos = docsAndPosEnum.nextPosition();
                        String term = bytesRef.utf8ToString();
                        invertedIndex.merge(
                                term,
                                mergeValue.apply(scoreDoc.doc, pos),
                                (s1, s2) -> {
                                    s1.addAll(s2);
                                    return s1;
                                });
                    }
                }

                invertedIndex.forEach((key, value) -> {
                    String row = key + ":" + value;
                    System.out.println(row);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
