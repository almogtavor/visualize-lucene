//package visualize.lucene;
//
//import org.apache.solr.client.solrj.SolrClient;
//import org.apache.solr.client.solrj.SolrQuery;
//import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.client.solrj.impl.Http2SolrClient;
//import org.apache.solr.client.solrj.impl.HttpSolrClient;
//import org.apache.solr.client.solrj.response.QueryResponse;
//import org.apache.solr.common.SolrDocument;
//import org.apache.solr.common.SolrDocumentList;
//import org.apache.solr.common.params.SolrParams;
//import org.apache.solr.common.util.NamedList;
//import org.apache.solr.request.SolrQueryRequest;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class SolrQueryFilterChecker {
//    public static void main(String[] args) throws SolrServerException, IOException {
//        // Set up the Solr client and execute the query
//        SolrClient solrClient = new Http2SolrClient.Builder("http://localhost:8983/solr").build();
//        SolrQueryRequest queryRequest = new SolrQueryR();
//        SolrQuery query = new SolrQuery();
//        query.setQuery("title:test AND body:example");
//        QueryResponse response = solrClient.query(query);
//        SolrDocumentList results = response.getResults();
//
//        // Find the parameters that are responsible for a document being returned
//        for (SolrDocument document : results) {
//            List<String> responsibleParams = findResponsibleParams(query.getParams());
//            System.out.println(responsibleParams);  // Output: [title:test, body:example]
//        }
//    }
//
//    public static List<String> findResponsibleParams(SolrParams params) {
//        List<String> responsibleParams = new ArrayList<>();
//        NamedList<Object> queryTree = (NamedList<Object>) params.get("query");
//        findResponsibleParams(queryTree, responsibleParams);
//        return responsibleParams;
//    }
//
//    public static void findResponsibleParams(NamedList<Object> queryTree, List<String> responsibleParams) {
//        if (queryTree == null) {
//            return;
//        }
//        for (int i = 0; i < queryTree.size(); i++) {
//            String key = queryTree.getName(i);
//            Object value = queryTree.getVal(i);
//            if ("field".equals(key)) {
//                String field = (String) value;
//                String fieldValue = queryTree.get("value").toString();
//                responsibleParams.add(field + ":" + fieldValue);
//            } else if (value instanceof NamedList) {
//                findResponsibleParams((NamedList<Object>) value, responsibleParams);
//            }
//        }
//    }
//}
