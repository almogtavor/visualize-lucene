package visualize.lucene.initialize;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;

public class Utils {
    public static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
        Document doc = new Document();
        FieldType type = new FieldType();
        type.setStoreTermVectors(true);
        type.setIndexOptions(IndexOptions.DOCS);
        // Optional
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorOffsets(true);
        doc.add(new Field("title", title, type));
        doc.add(new Field("isbn", isbn, type));
        w.addDocument(doc);
    }

    public static void addDocForIndexVis(IndexWriter w, String title, String isbn) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));

        // use a string field for isbn because we don't want it tokenized
        doc.add(new StringField("isbn", isbn, Field.Store.YES));
        w.addDocument(doc);
    }
}
