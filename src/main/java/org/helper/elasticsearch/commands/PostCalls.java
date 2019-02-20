package org.helper.elasticsearch.commands;

import java.io.IOException;

import org.helper.elasticsearch.model.ScrollResponse;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostCalls {
	public String ES_URL;
	
	public PostCalls(String esURL) {
		this.ES_URL=esURL;
	}
		
	
	public ScrollResponse makeScanCall(String scrollID, String keepAliveTime) throws IOException {
		ScrollResponse retVal=new ScrollResponse();
		OkHttpClient client = new OkHttpClient();

		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, "{\n    \"scroll\" : \"" +keepAliveTime+ "m\", \n    \"scroll_id\" : \""
				+ scrollID
				+ "\" \n}");
		Request request = new Request.Builder()
		  .url(this.ES_URL + "/scroll")
		  .post(body)
		  .addHeader("Content-Type", "application/json")
		  .addHeader("Accept", "application/json")
		  .addHeader("cache-control", "no-cache")
		  .addHeader("Postman-Token", "3d220bd7-f0dc-4d26-b907-2218406f7028")
		  .build();

		Response response = client.newCall(request).execute();
		
		
		JSONObject responseObj = new JSONObject(response.body().string());
		retVal.setScrollID(responseObj.getString("_scroll_id"));
		Integer totalHitsCount = responseObj.getJSONObject("hits").getInt("total");
		retVal.setSearchResults(responseObj.getJSONObject("hits"));
		retVal.setTotalHitsCount(totalHitsCount);
		
		return retVal;
	}
	
	public ScrollResponse makeScrollCall(String query, String keepAliveTime) throws IOException {
		ScrollResponse retVal=new ScrollResponse();
		OkHttpClient client = new OkHttpClient();

		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, query);
		Request request = new Request.Builder()
		  .url(this.ES_URL+ "?scroll=" + keepAliveTime + "m")
		  .post(body)
		  .addHeader("Content-Type", "application/json")
		  .addHeader("Accept", "application/json")
		  .addHeader("cache-control", "no-cache")
		  .build();

		Response response = client.newCall(request).execute();
		JSONObject responseObj = new JSONObject(response.body().string());
		retVal.setScrollID(responseObj.getString("_scroll_id"));
		Integer totalHitsCount = responseObj.getJSONObject("hits").getInt("total");
		retVal.setSearchResults(responseObj.getJSONObject("hits"));
		retVal.setTotalHitsCount(totalHitsCount);
		
		return retVal;
	}

	
	public String makeSearchCall(String query) throws IOException {
		
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, query);
		Request request = new Request.Builder()
		  .url(this.ES_URL)
		  .post(body)
		  .addHeader("Content-Type", "application/json")
		  .addHeader("Accept", "application/json")
		  .addHeader("cache-control", "no-cache")
		  .addHeader("Postman-Token", "b101efd9-5199-4555-8de0-54567eddf1ea")
		  .build();

		Response response = client.newCall(request).execute();
		
		
		return response.body().string();
		
	}
}
