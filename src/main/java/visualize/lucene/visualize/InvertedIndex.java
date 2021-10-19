package visualize.lucene.visualize;

import com.jakewharton.picnic.Table;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

public class InvertedIndex {
    private static final Logger logger = LogManager.getRootLogger();
    @Getter
    private static Map<String, Set<String>> invertedIndex;

    public InvertedIndex(IndexSearcher searcher, IndexReader reader, Query query, TopDocs topDocs) {
        logger.info("almog");
        calculateIndex(searcher, reader, query, topDocs);
    }

    public void calculateIndex(IndexSearcher searcher, IndexReader reader, Query query, TopDocs topDocs) {
        try {
//            MatchAllDocsQuery query = new MatchAllDocsQuery();
//            TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
            BiFunction<Integer, Integer, Set<String>> mergeValue =
                    (docId, pos) -> {
                        TreeSet<String> treeSet = new TreeSet<>();
                        treeSet.add((docId + 1) + ":" + pos);
                        return treeSet;
                    };

            invertedIndex = new HashMap<>();
//            for (ScoreDoc scoreDoc : hits.scoreDocs) {
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Fields termVs = reader.getTermVectors(scoreDoc.doc);
                Terms terms = termVs.terms("title");
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void visualize() {
        invertedIndex.forEach((key, value) -> {
            String row = key + ":" + value;
            System.out.println(row);
        });
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

                visualize();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
