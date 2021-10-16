package visualize.lucene.calculate;

import lombok.Getter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import visualize.lucene.Inverted;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class InvertedIndex {
    private static final Logger logger = Logger.getLogger(String.valueOf(Inverted.class));
    @Getter
    private static Map<String, Set<String>> invertedIndex;
    private static Path path;

    public InvertedIndex(Path path) {
        this.path = path;
    }

    public void vis(String path) {
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

                invertedIndex = new HashMap<>();
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

                visualizeInvertedIndex(invertedIndex);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void visualizeInvertedIndex(Map<String, Set<String>> invertedIndex) {
        invertedIndex.forEach((key, value) -> {
            String row = key + ":" + value;
            System.out.println(row);
        });
    }
}
