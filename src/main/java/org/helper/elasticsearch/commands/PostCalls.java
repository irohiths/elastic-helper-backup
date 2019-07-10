package org.helper.elasticsearch.commands;

import java.io.IOException;

import org.helper.elasticsearch.model.ScrollResponse;
import org.helper.elasticsearch.model.SearchResponse;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostCalls {

	public ScrollResponse makeScanCall(String scrollID, String keepAliveTime, String esURL) throws IOException {
		ScrollResponse retVal=new ScrollResponse();
		OkHttpClient client = new OkHttpClient();

//    	System.out.println("Scan esURL : " + esURL);
		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, "{\n    \"scroll\" : \"" +keepAliveTime+ "m\", \n    \"scroll_id\" : \""
				+ scrollID
				+ "\" \n}");
		Request request = new Request.Builder()
		  .url(esURL + "_search/scroll")
		  .post(body)
		  .addHeader("Content-Type", "application/json")
		  .addHeader("Accept", "application/json")
		  .addHeader("Authorization", "Basic ZWxhc3RpYzppT0RIdE84bXJTaWp4RVRoVFhleA==")
		  .addHeader("cache-control", "no-cache")
		  .addHeader("Postman-Token", "3d220bd7-f0dc-4d26-b907-2218406f7028")
		  .build();

		Response response = client.newCall(request).execute();
		
		String esResponse = response.body().string();
//    	System.out.println("Scan esResponse : " + esResponse);
		JSONObject responseObj = new JSONObject(esResponse);
		retVal.setScrollID(responseObj.getString("_scroll_id"));
		Integer totalHitsCount = responseObj.getJSONObject("hits").getInt("total");
		retVal.setSearchResults(responseObj.getJSONObject("hits"));
		retVal.setTotalHitsCount(totalHitsCount);
		
		return retVal;
	}
	
	public ScrollResponse makeScrollCall(String query, String keepAliveTime, String esURL) throws IOException {
		ScrollResponse retVal=new ScrollResponse();
		OkHttpClient client = new OkHttpClient();

		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, query);
		Request request = new Request.Builder()
		  .url(esURL+ "/_search?scroll=" + keepAliveTime + "m")
		  .post(body)
		  .addHeader("Content-Type", "application/json")
		  .addHeader("Authorization", "Basic ZWxhc3RpYzppT0RIdE84bXJTaWp4RVRoVFhleA==")
		  .addHeader("Accept", "application/json")
		  .addHeader("cache-control", "no-cache")
		  .build();

		Response response = client.newCall(request).execute();
		String esResponse = response.body().string();
//		System.out.println("Scroll esResponse : " + esResponse);
		JSONObject responseObj = new JSONObject(esResponse);
		retVal.setScrollID(responseObj.getString("_scroll_id"));
		Integer totalHitsCount = responseObj.getJSONObject("hits").getInt("total");
		retVal.setSearchResults(responseObj.getJSONObject("hits"));
		retVal.setTotalHitsCount(totalHitsCount);
		
		return retVal;
	}

	
	public SearchResponse makeSearchCall(String query, String esURL) throws IOException {
		SearchResponse retVal = new SearchResponse();

		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, query);
		Request request = new Request.Builder()
		  .url(esURL + "/_search")
		  .post(body)
		  .addHeader("Content-Type", "application/json")
		  .addHeader("Authorization", "Basic ZWxhc3RpYzppT0RIdE84bXJTaWp4RVRoVFhleA==")
		  .addHeader("Accept", "application/json")
		  .addHeader("cache-control", "no-cache")
		  .build();

		Response response = client.newCall(request).execute();

		JSONObject responseObj = new JSONObject(response.body().string());
		retVal.setSearchResults(responseObj.getJSONObject("hits"));
		Integer totalHitsCount = responseObj.getJSONObject("hits").getInt("total");
		retVal.setTotalHitsCount(totalHitsCount);
		return retVal;
		
	}
}
