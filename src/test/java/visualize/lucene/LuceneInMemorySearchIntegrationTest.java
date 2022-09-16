package visualize.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.junit.Test;

public class LuceneInMemorySearchIntegrationTest {

    @Test
    public void givenSearchQueryWhenFetchedDocumentThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("Hello world", "Some hello world ");

        List<Document> documents = inMemoryLuceneIndex.searchIndex("body", "world");

        Assert.assertEquals("Hello world", documents.get(0).get("title"));
    }

    @Test
    public void givenTermQueryWhenFetchedDocumentThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("activity", "running in track");
        inMemoryLuceneIndex.indexDocument("activity", "Cars are running on road");

        Term term = new Term("body", "running");
        Query query = new TermQuery(term);

        List<Document> documents = inMemoryLuceneIndex.searchIndex(query);
        Assert.assertEquals(2, documents.size());
    }

    @Test
    public void givenPrefixQueryWhenFetchedDocumentThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("article", "Lucene introduction");
        inMemoryLuceneIndex.indexDocument("article", "Introduction to Lucene");

        Term term = new Term("body", "intro");
        Query query = new PrefixQuery(term);

        List<Document> documents = inMemoryLuceneIndex.searchIndex(query);
        Assert.assertEquals(2, documents.size());
    }

    @Test
    public void givenBooleanQueryWhenFetchedDocumentThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("Destination", "Las Vegas singapore car");
        inMemoryLuceneIndex.indexDocument("Commutes in singapore", "Bus Car Bikes");

        Term term1 = new Term("body", "singapore");
        Term term2 = new Term("body", "car");

        TermQuery query1 = new TermQuery(term1);
        TermQuery query2 = new TermQuery(term2);

        BooleanQuery booleanQuery = new BooleanQuery.Builder().add(query1, BooleanClause.Occur.MUST)
                .add(query2, BooleanClause.Occur.MUST).build();

        List<Document> documents = inMemoryLuceneIndex.searchIndex(booleanQuery);
        Assert.assertEquals(1, documents.size());
    }

    @Test
    public void givenPhraseQueryWhenFetchedDocumentThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("quotes", "A rose by any other name would smell as sweet.");

        Query query = new PhraseQuery(1, "body", new BytesRef("smell"), new BytesRef("sweet"));
        List<Document> documents = inMemoryLuceneIndex.searchIndex(query);

        Assert.assertEquals(1, documents.size());
    }

    @Test
    public void givenFuzzyQueryWhenFetchedDocumentThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("article", "Halloween Festival");
        inMemoryLuceneIndex.indexDocument("decoration", "Decorations for Halloween");

        Term term = new Term("body", "hallowen");
        Query query = new FuzzyQuery(term);

        List<Document> documents = inMemoryLuceneIndex.searchIndex(query);
        Assert.assertEquals(2, documents.size());
    }

    @Test
    public void givenWildCardQueryWhenFetchedDocumentThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("article", "Lucene introduction");
        inMemoryLuceneIndex.indexDocument("article", "Introducing Lucene with Spring");

        Term term = new Term("body", "intro*");
        Query query = new WildcardQuery(term);

        List<Document> documents = inMemoryLuceneIndex.searchIndex(query);
        Assert.assertEquals(2, documents.size());
    }

    @Test
    public void givenSortFieldWhenSortedThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("Ganges", "River in India");
        inMemoryLuceneIndex.indexDocument("Mekong", "This river flows in south Asia");
        inMemoryLuceneIndex.indexDocument("Amazon", "Rain forest river");
        inMemoryLuceneIndex.indexDocument("Rhine", "Belongs to Europe");
        inMemoryLuceneIndex.indexDocument("Nile", "Longest River");

        Term term = new Term("body", "river");
        Query query = new WildcardQuery(term);

        SortField sortField = new SortField("title", SortField.Type.STRING_VAL, false);
        Sort sortByTitle = new Sort(sortField);

        List<Document> documents = inMemoryLuceneIndex.searchIndex(query, sortByTitle);
        Assert.assertEquals(4, documents.size());
        Assert.assertEquals("Amazon", documents.get(0).getField("title").stringValue());
    }

    @Test
    public void whenDocumentDeletedThenCorrect() throws IOException {
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(new MMapDirectory(Path.of("/test_dir")), new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("Ganges", "River in India");
        inMemoryLuceneIndex.indexDocument("Mekong", "This river flows in south Asia");

        Term term = new Term("title", "ganges");
        inMemoryLuceneIndex.deleteDocument(term);

        Query query = new TermQuery(term);

        List<Document> documents = inMemoryLuceneIndex.searchIndex(query);
        Assert.assertEquals(0, documents.size());
    }

}
