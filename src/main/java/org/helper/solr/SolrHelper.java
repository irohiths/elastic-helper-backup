package org.helper.solr;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.helper.elasticsearch.model.SearchResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

public class SolrHelper {

    public SearchResponse getSolrDocuments(Integer totalDocs){

        try{
            SearchResponse retVal = new SearchResponse();
            JSONArray volOpDocsArray = new JSONArray();
            OkHttpClient client = new OkHttpClient();

            Integer count = 0;
            String cursorMarkValue = URLEncoder.encode("*", "UTF-8");

            while(count<totalDocs) {

                System.out.println("retrieving solr records cursorMarkValue : " + cursorMarkValue);
                Request request = new Request.Builder()
//                    .url("http://event-repostg.handsonconnect.org:8983/solr/pcevents/select?wt=json&q=%2A:%2A&sort=synchModstamp%20ASC,id%20ASC&rows=100&cursorMark=%2A")
                        .url("http://event-repostg.handsonconnect.org:8983/solr/pcevents/select?wt=json&q=%2A:%2A&sort=synchModstamp%20ASC,id%20ASC&rows=100&cursorMark=" + cursorMarkValue)
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
                System.out.println("solr  response : " + responseBody.toString());
                JSONArray volOpDocs = responseBody.getJSONObject("response").getJSONArray("docs");

                for(int i = 0; i < volOpDocs.length() ; i++){
                    volOpDocsArray.put(count,volOpDocs.getJSONObject(i));
                    count++;
                }
                System.out.println("retrieved " + count + " records from Solr");
                cursorMarkValue = URLEncoder.encode(responseBody.getString("nextCursorMark"), "UTF-8");
            }


            retVal.setSearchResults(new JSONObject().put("solrReponse", volOpDocsArray));
            retVal.setTotalHitsCount(count);
            return retVal;

        } catch (IOException e) {
            System.out.println("Caught IOException while retrieving Solr documents");
        }
        return null;
    }
}
