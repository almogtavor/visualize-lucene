package visualize.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LuceneQueryFilterFinder {
    public static void main(String[] args) throws Exception {
        // Set up the index and search it
        Directory directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter indexWriter = new IndexWriter(directory, config);
        indexWriter.close();

        QueryParser queryParser = new QueryParser("field", new StandardAnalyzer());
        Query query = queryParser.parse("(field1:value1 AND field2:value2) OR field3:value3");

        IndexWriterConfig newConfig = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter newIndexWriter = new IndexWriter(directory, newConfig);
        Document doc1 = new Document();
        doc1.add(new TextField("field1", "A", Field.Store.YES));
        doc1.add(new TextField("field2", "value2", Field.Store.YES));
        doc1.add(new TextField("field3", "value3", Field.Store.YES));
        newIndexWriter.addDocument(doc1);
        Document doc2 = new Document();
        doc2.add(new TextField("field1", "value1", Field.Store.YES));
        doc2.add(new TextField("field2", "B", Field.Store.YES));
        doc2.add(new TextField("field3", "value3", Field.Store.YES));
        newIndexWriter.addDocument(doc2);
        Document doc3 = new Document();
        doc3.add(new TextField("field1", "value1", Field.Store.YES));
        doc3.add(new TextField("field2", "value2", Field.Store.YES));
        doc3.add(new TextField("field3", "C", Field.Store.YES));
        newIndexWriter.addDocument(doc3);
        long commitLong = newIndexWriter.commit();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        TopDocs topDocs = indexSearcher.search(query, 10);
        int numDocs = ((int) topDocs.totalHits.value);

        // Find the parameters that are responsible for a document being returned
        for (int i = 0; i < numDocs; i++) {
            int docId = topDocs.scoreDocs[i].doc;
            Document document = indexSearcher.doc(docId);
            List<String> responsibleParams = findResponsibleParams(document, query, indexReader, docId, indexSearcher);
            System.out.println(responsibleParams);
        }
    }
    public static List<String> findResponsibleParams(Document document, Query query, IndexReader indexReader, int docId, IndexSearcher indexSearcher) {
        List<String> responsibleParams = new ArrayList<>();

        if (query instanceof BooleanQuery) {
            try {
                if (indexSearcher.explain(query, docId).isMatch()) {
                    BooleanQuery booleanQuery = (BooleanQuery) query;
                    for (BooleanClause clause : booleanQuery.clauses()) {
                        Query subQuery = clause.getQuery();
                        List<String> subQueryParams = findResponsibleParams(document, subQuery, indexReader, docId, indexSearcher);
                        if (!subQueryParams.isEmpty()) {
                            responsibleParams.addAll(subQueryParams);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Check if the document matches the current query
            try {
                if (indexSearcher.explain(query, docId).isMatch()) {
                    responsibleParams.add(query.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return responsibleParams;
    }

}