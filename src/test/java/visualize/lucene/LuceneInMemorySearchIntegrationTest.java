package visualize.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.ProximityQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessor;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.surround.query.AndQuery;
import org.apache.lucene.queryparser.xml.QueryBuilderFactory;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
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
        ByteBuffersDirectory memoryIndex = new ByteBuffersDirectory();
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(memoryIndex, new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("Ganges", "River in India");
        inMemoryLuceneIndex.indexDocumentWithTwoFields("Mekong", "This river flows in south Asia", "zoo");
        inMemoryLuceneIndex.indexDocument("Amazon", "Rain forest river");
        inMemoryLuceneIndex.indexDocumentWithTwoFields("Rhine", "Belongs to Europe", "zoo");
        inMemoryLuceneIndex.indexDocumentWithTwoFields("Nile", "Longest River", "zoo");


        TermQuery query1 = new TermQuery(new Term("body", "river"));
        TermQuery query2 = new TermQuery(new Term("bla", "zoo"));

        BooleanQuery booleanQuery = new BooleanQuery.Builder().add(query1, BooleanClause.Occur.MUST)
                .add(query2, BooleanClause.Occur.FILTER).build();
        booleanQuery.visit(new QueryVisitor() {
            @Override
            public QueryVisitor getSubVisitor(BooleanClause.Occur occur, Query parent) {
                return super.getSubVisitor(occur, parent);
            }
        });
        SortField sortField = new SortField("title", SortField.Type.STRING_VAL, false);
        Sort sortByTitle = new Sort(sortField);

//        List<Document> documents = inMemoryLuceneIndex.searchIndex(query, sortByTitle);

        IndexReader indexReader = DirectoryReader.open(memoryIndex);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(booleanQuery, 10);
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
            System.out.println(searcher.explain(booleanQuery, scoreDoc.doc));
        }
        Assert.assertEquals(2, documents.size());
//        Assert.assertEquals("Amazon", documents.get(0).getField("title").stringValue());
    }


    @Test
    public void givenSortFieldWhenSortedThenCorrect2() throws IOException {
        ByteBuffersDirectory memoryIndex = new ByteBuffersDirectory();
        InMemoryLuceneIndex inMemoryLuceneIndex = new InMemoryLuceneIndex(memoryIndex, new StandardAnalyzer());
        inMemoryLuceneIndex.indexDocument("Ganges", "River in India");
        inMemoryLuceneIndex.indexDocumentWithTwoFields("Mekong", "This river flows in south Asia", "zoo");
        inMemoryLuceneIndex.indexDocument("Amazon", "Rain forest river");
        inMemoryLuceneIndex.indexDocumentWithTwoFields("Rhine", "Belongs to Europe", "zoo");
        inMemoryLuceneIndex.indexDocumentWithTwoFields("Nile", "Longest River", "zoo");


        TermQuery query1 = new TermQuery(new Term("body", "river"));
        TermQuery query2 = new TermQuery(new Term("bla", "zoo"));

        BooleanQuery booleanQuery = new BooleanQuery.Builder().add(query1, BooleanClause.Occur.MUST)
                .add(query2, BooleanClause.Occur.FILTER).build();
        booleanQuery.visit(new QueryVisitor() {
            @Override
            public QueryVisitor getSubVisitor(BooleanClause.Occur occur, Query parent) {
                return super.getSubVisitor(occur, parent);
            }
        });
        SortField sortField = new SortField("title", SortField.Type.STRING_VAL, false);
        Sort sortByTitle = new Sort(sortField);

//        List<Document> documents = inMemoryLuceneIndex.searchIndex(query, sortByTitle);

        IndexReader indexReader = DirectoryReader.open(memoryIndex);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(booleanQuery, 10);
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
            System.out.println(searcher.explain(booleanQuery, scoreDoc.doc));
        }
        try {
            final String def = "content";
            SyntaxParser parser = new StandardQueryParser().getSyntaxParser();
            QueryNodeProcessor processor = new StandardQueryParser().getQueryNodeProcessor();
            QueryBuilder builder = new StandardQueryParser().getQueryBuilder();

            QueryNode q = new ProximityQueryNode(
                    Arrays.<QueryNode>asList(new org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode(
                            def, "foo bar", 0, "foo bar".length())),
                    def, ProximityQueryNode.Type.NUMBER, 3, false);

            ArrayList<QueryNode> parsed = new ArrayList<>();
            //         parsed.add(parser.parse("\"test boo\"~2", def));
            //         parsed.add(parser.parse("confusion~2", def));
            //         parsed.add(parser.parse("foo AND bar", def));
            //         parsed.add(parser.parse("(optional OR keuze) AND (field OR metastase)", def));
            parsed.add(parser.parse("(\"letter comes\"~3 \"agitated employees\"~4 \"worried about salaries\"~1)"
                    + " AND (\"letter comes agitated employees worried about salaries\"~8)", def));
            parsed.add(parser.parse("\"d.d\"", def));

            ArrayList<QueryNode> queries = new ArrayList<>();
            queries.addAll(parsed);
            //         queries.add(q);

            //         queries.add(new OrQueryNode(parsed));
            //         queries.add(new ProximityQueryNode(parsed, def, ProximityQueryNode.Type.NUMBER, 5, false));
            //         queries.add(new ProximityQueryNode(Arrays.asList(queries.get(3), queries.get(4)), null, ProximityQueryNode.Type.NUMBER, 8, false));

            for (QueryNode n : queries) {
                System.out.println(n.toString());
                System.out.println();
                System.out.println(processor.process(n).toString());
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println();
            }
        } catch (QueryNodeException ex) {
            throw new Error(ex);
        }
        Assert.assertEquals(2, documents.size());
//        Assert.assertEquals("Amazon", documents.get(0).getField("title").stringValue());
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
