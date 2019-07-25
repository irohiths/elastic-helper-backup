package org.helper.solr;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.helper.app.App;
import org.helper.elasticsearch.model.SearchResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public class SolrHelper {

    public void getSolrDocuments(
            Integer totalDocs,
            BulkProcessor bulkProcessor,
            String indexName){

        try{
//            SearchResponse searchResponse = new SearchResponse();
            OkHttpClient client = new OkHttpClient();

            Integer count = 0;
            String cursorMarkValue = URLEncoder.encode("*", "UTF-8");

            while(count<totalDocs) {
                JSONArray volOpDocsArray = new JSONArray();
                SearchResponse searchResponse = new SearchResponse();

//                System.out.println("retrieving solr records cursorMarkValue : " + cursorMarkValue);
                Request request = new Request.Builder()
//                    .url("http://event-repostg.handsonconnect.org:8983/solr/pcevents/select?wt=json&q=%2A:%2A&sort=synchModstamp%20ASC,id%20ASC&rows=100&cursorMark=%2A")
                        .url("http://event-repostg.handsonconnect.org:8983/solr/pcevents/select?wt=json&q=%2A:%2A&sort=synchModstamp%20ASC,id%20ASC&rows=1000&cursorMark=" + cursorMarkValue)
                        .get()
                        .addHeader("Authorization", "Basic c3BjOkJlbmV2TzFlbmMz")
                        .addHeader("User-Agent", "PostmanRuntime/7.13.0")
                        .addHeader("Accept", "*/*")
                        .addHeader("Cache-Control", "no-cache")
                        .addHeader("Postman-Token", "c0762438-e322-4d87-b371-799468170a2e,2b13b084-e70f-4cec-a982-bc3d270edea6")
                        .addHeader("Host", "event-repostg.handsonconnect.org:8983")
                        .addHeader("accept-encoding", "gzip, deflate")
                        .addHeader("Connection", "keep-alive")
                        .addHeader("cache-control", "no-cache")
                        .build();

                Response response = client.newCall(request).execute();
                JSONObject responseBody = new JSONObject(response.body().string());
//                System.out.println("solr  response : " + responseBody.toString());
                JSONArray volOpDocs = responseBody.getJSONObject("response").getJSONArray("docs");

                for(int i = 0; i < volOpDocs.length() ; i++){
                    volOpDocsArray.put(i,volOpDocs.getJSONObject(i));
                    count++;
                }
                System.out.println("retrieved " + count + " records from Solr");
                cursorMarkValue = URLEncoder.encode(responseBody.getString("nextCursorMark"), "UTF-8");

                searchResponse.setSearchResults(new JSONObject().put("solrReponse", volOpDocsArray));
                searchResponse.setTotalHitsCount(count);
                processSolrResponse(searchResponse, bulkProcessor, indexName);
            }
//            retVal.setSearchResults(new JSONObject().put("solrReponse", volOpDocsArray));
//            retVal.setTotalHitsCount(count);
//            return retVal;

            // Close bulkProcessor
            bulkProcessor.awaitClose(200, TimeUnit.SECONDS);

        } catch (IOException e) {
            System.out.println("Caught IOException while retrieving Solr documents");
        } catch (InterruptedException e) {
            System.out.println("Encountered BulkProcessor InterruptedException :" + e.getMessage());
            e.printStackTrace();
        }
//        return null;
    }

    private static void processSolrResponse(
            SearchResponse esSearchResponse,
            BulkProcessor bulkProcessor,
            String indexName)
            throws IOException, InterruptedException {

        // find total hits, we need to process this many records
        Integer totalHitsCount = esSearchResponse.getTotalHitsCount();
        System.out.println("totalHitsCount : " + totalHitsCount);
        // set start record counter to 1
        Integer currentCount = 1;

        // first set of results with Scroll call
        JSONObject searchResults = esSearchResponse.getSearchResults();
        JSONArray hitsArray = searchResults.getJSONArray("solrReponse");
        System.out.println("indexing solr reponses hitsArray.length() : " + hitsArray.length());
        try {
            for (int i = 0; i < hitsArray.length(); i++) {

                // Strip Record and get info required to upsert
                JSONObject sourceObj = hitsArray.getJSONObject(i);
                //				JSONObject sourceObj = aRecordObj.getJSONObject("_source");
                String indexType = "_doc";
                currentCount++;
                sourceObj.remove("id");
                //				System.out.println("hit " + (currentCount) + " : "
                //						+ " type : " + indexType
                //						+ " indexName : " + indexName
                //						+ " record : " + sourceObj.toString()
                //				);

                sourceObj = App.populateMissingFields(sourceObj, indexName);
                // Insert record into local ES instance
                IndexRequest indexRequest = new IndexRequest(indexName, indexType);

                indexRequest.source(sourceObj.toMap());
                bulkProcessor.add(indexRequest);
            }
        } catch (JSONException e) {
            System.out.println("Caught Error while processing Solr search response " + e.getMessage());
        }

        System.out.println("Total Records Processed : " + (currentCount - 1));
    }
}
