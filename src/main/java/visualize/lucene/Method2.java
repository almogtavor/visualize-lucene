package visualize.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class Method2 {
    private static final Logger logger = Logger.getLogger(String.valueOf(Inverted.class));

    static void printWholeIndex(IndexSearcher searcher) throws IOException {
        MatchAllDocsQuery query = new MatchAllDocsQuery();
        TopDocs hits = searcher.search(query, Integer.MAX_VALUE);

        Map<String, Set<Integer>> invertedIndex = new HashMap<>();

        if (null == hits.scoreDocs || hits.scoreDocs.length <= 0) {
            logger.info("No Hits Found with MatchAllDocsQuery");
            return;
        }

        for (ScoreDoc hit : hits.scoreDocs) {
            Document doc = searcher.doc(hit.doc);

            List<IndexableField> allFields = doc.getFields();

            for(IndexableField field:allFields){
                // Single document inverted index
                Terms terms = searcher.getIndexReader().getTermVector(hit.doc,field.name());

                if (terms != null )  {
                    TermsEnum termsEnum = terms.iterator();
                    while(termsEnum.next() != null){
                        if(invertedIndex.containsKey(termsEnum.term().utf8ToString())){
                            Set<Integer> existingDocs = invertedIndex.get(termsEnum.term().utf8ToString());
                            existingDocs.add(hit.doc);
                            invertedIndex.put(termsEnum.term().utf8ToString(),existingDocs);

                        }else{
                            Set<Integer> docs = new TreeSet<>();
                            docs.add(hit.doc);
                            invertedIndex.put(termsEnum.term().utf8ToString(), docs);
                        }
                    }
                }
            }
        }

        logger.info("Printing Inverted Index:");

        invertedIndex.forEach((key , value) -> {
            String row = key + ":" + value;
            System.out.println(row);
        });
    }

}
