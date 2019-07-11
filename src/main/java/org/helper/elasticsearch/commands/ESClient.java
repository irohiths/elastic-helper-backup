package org.helper.elasticsearch.commands;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.lucene.index.ExitableDirectoryReader;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ESClient {
	
	public String serverName;
	public Integer portNum;
	public String clusterName;
	public Integer bulkSize;
	public RestHighLevelClient esClient=null;
	
	public ESClient(
			String serverName,
			Integer portNum,
			String clusterName,
			Integer bulkSize) {
		
		this.serverName=serverName;
		this.portNum=portNum;
		this.clusterName=clusterName;
		this.bulkSize=bulkSize;
		
	}
	
	public BulkProcessor getBulkProcessor() {

		RestHighLevelClient client= getElasticServerClient(
				serverName, 
				portNum, 
				clusterName);
		BulkProcessor bp = createBulkProcessor(client,bulkSize);

		return bp; 
	}
	
	public RestHighLevelClient getElasticServerClient(String serverName, Integer portNum, String clusterName) {
		RestHighLevelClient client = null;
		try {
			System.out.println("Creating ES Client : " 
					+ "serverName " + serverName
					+ " portNum " +  portNum);

      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          AuthScope.ANY, new UsernamePasswordCredentials("elastic", "iODHtO8mrSijxEThTXex\n"));

			client = new RestHighLevelClient(
					RestClient.builder(new HttpHost(InetAddress.getByName(serverName), portNum, "https"))
					.setDefaultHeaders(
					new Header[] {
							new BasicHeader(
									"Authorization",
									"Basic " + "ZWxhc3RpYzppT0RIdE84bXJTaWp4RVRoVFhleA==")})
//							.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
//								@Override
//								public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
//									return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
//								}
//							})
			);
			esClient=client;
//			client = new RestHighLevelClient(RestClient.builder(new HttpHost("127.0.0.1", portNum, "http")));
			
//			//testing client using a get Request
//			GetRequest getRequest = new GetRequest();
//			GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
//			if(getResponse.isExists()) {
//				System.out.println("Testing ES Client...");
//				System.out.println("ES Client Working Received Response Source : " + getResponse.getSourceAsString());
//			}else {
//				System.out.println("Error could ES Client Test Failed Exiting...");
//				System.exit(0);
//			}
			
		} catch (UnknownHostException e) {
			System.out.println("UnknownHostException Error while creating ES Client : " + e.getMessage());
			e.printStackTrace();
			System.exit(0);;
		} catch (IOException e) {
			System.out.println("IOException Error while creating ES Client : " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
	
		return client;
	}
	
	public RestHighLevelClient getEsClient() {
		return esClient;
	}

	public void setEsClient(RestHighLevelClient esClient) {
		this.esClient = esClient;
	}

	private BulkProcessor createBulkProcessor(RestHighLevelClient client, Integer bulkSize) {
		
		BulkProcessor.Listener listener = new BulkProcessor.Listener() {
			
			
			public void beforeBulk(long executionId, BulkRequest request) {
				// TODO Auto-generated method stub
				
			}
			
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
				if(null!=failure) {
					System.out.println("Error: Bulk Processor encountered an error : " 
						+ failure.getMessage() 
						+ " failure.getCause() :" + failure.getCause()
						+ " failure Reason : " + failure.toString()
						+ " executionID : " + executionId
						+ " number of actions : " + request.numberOfActions());
					List<Object> payloads = request.payloads();
					
					if(null!=payloads) {
						for(Object aPayload: payloads) {
							System.out.println(aPayload);
						}
					}
				}
				
			}
			
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				// check for errors in bulk response
				if(response.hasFailures()) {
					System.out.println("Bulk Processor encountered an error : " 
				+ response.buildFailureMessage()
				+ " bulk processor executionID : " + executionId);
					BulkItemResponse errorList[] = response.getItems();
					int errorSize=errorList.length;
					if(errorSize>0) {
						System.out.println("First Error message from BulkResponse : " + errorList[0].getFailureMessage()
								+ " Bulk Response Index : " + errorList[0].getIndex());
					}
				}
				
				
				
			}
		};
				
		BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
		        (request, bulkListener) ->
		            client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener);
		
		
		BulkProcessor bulkProcessor =
				BulkProcessor.builder(
						bulkConsumer,
//						client::bulkAsync,
						listener)
				.setBulkActions(bulkSize)
				.setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB))
				.setFlushInterval(TimeValue.timeValueSeconds(5))
				.setConcurrentRequests(1)
				.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100),3))
				.build();
		return bulkProcessor;
	}
	
	public static Map<String, Object> toMap(JSONObject object) throws JSONException {
		Map<String, Object> map = new HashMap<String, Object>();

		Iterator<String> keysItr = object.keys();
		while(keysItr.hasNext()) {
			String key = keysItr.next();
			Object value = object.get(key);

			if(value instanceof JSONArray) {
				value = toList((JSONArray) value);
			}

			else if(value instanceof JSONObject) {
				value = toMap((JSONObject) value);
			}
			map.put(key, value);
		}
		return map;
	}
	
	public static List<Object> toList(JSONArray array) throws JSONException {
		List<Object> list = new ArrayList<Object>();
		for(int i = 0; i < array.length(); i++) {
			Object value = array.get(i);
			if(value instanceof JSONArray) {
				value = toList((JSONArray) value);
			}

			else if(value instanceof JSONObject) {
				value = toMap((JSONObject) value);
			}
			list.add(value);
		}
		return list;
	}
}
