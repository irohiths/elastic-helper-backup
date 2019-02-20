package org.helper.elasticsearch.elastic_helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

import org.helper.elasticsearch.commands.PostCalls;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.helper.elasticsearch.commands.BaseIndexer;
import org.helper.elasticsearch.commands.ESClient;
import org.helper.elasticsearch.model.ScrollResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

/**
 * Query ES cluster and update nationalIdentifier of the records that match
 *
 */
public class App 
{
	public static PostCalls esPostCallObj;
	public static final Pattern PUNCTUATION = Pattern.compile("[^0-9\\s\\p{L}]");
	public static final Pattern WHITE_SPACES = Pattern.compile("\\s");
	
	public static void main( String[] args )
	{

		//Elasticsearch server Settings for Upsert
		String esServerName = "10.241.17.6";
		String clusterName = "DarwinESCluster_DEV";
		Integer esPortNum = 9200;
		Integer bulkSize = 1000;

		System.out.println( "ES Cluster Record Updater" );
		String esURL = "http://"+esServerName+ ":9200/_search";
		String country = "italy";
		String natIDTypeCode = "00021";
		Integer hitsSize = 500;
		
		
		
		
		ESClient esClientObj = new ESClient(
				esServerName,
				esPortNum,
				clusterName,
				bulkSize);
		
//		RestHighLevelClient theClient=BaseIndexer.getElasticServerClient(esServerName, 9200, clusterName);
		
		esPostCallObj = new PostCalls(esURL);
		String query = "{\"size\":"+ hitsSize + ",\"query\":{\"bool\":{\"must\":[{\"term\":{\"gdmi.countryName\":\"" + country + "\"}},{\"term\":{\"gdmi.nationalIdentifierTypeCode\":\""+ natIDTypeCode+ "\"}}]}}}";
		try {
			ScrollResponse scrollResponseObj = esPostCallObj.makeScrollCall(query,"5");
			
			processScanAndScroll(scrollResponseObj, esClientObj.getBulkProcessor(),esClientObj);
			
			
//			RestHighLevelClient esClient = esClientObj.getEsClient();
			
//			if(esClient!=null) {
//				System.out.println("Closing ES Client");
//				esClient.close();
//			}
			
			System.out.println("Finished Running Record Updater!");
		} catch (IOException e) {
			System.out.println("Encountered IOException :" + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("Encountered BulkProcessor InterruptedException :" + e.getMessage());
			e.printStackTrace();
		}

	}
	
	private static void processScanAndScroll(ScrollResponse scrollResponseObj, BulkProcessor bulkProcessor, ESClient esClientObj) throws IOException, InterruptedException {
		
		//find total hits, we need to process this many records
		Integer totalHitsCount = scrollResponseObj.getTotalHitsCount();
		System.out.println("totalHitsCount : " + totalHitsCount);
		//set start record counter to 1
		Integer currentCount = 1;
		
		//first set of results with Scroll call
		JSONObject scrollResults = scrollResponseObj.getSearchResults();
		JSONArray hitsArray = scrollResults.getJSONArray("hits");
		String scrollId =scrollResponseObj.getScrollID();
		
		
		
		
		while(currentCount<=totalHitsCount) {
			for(int i=0;i<hitsArray.length();i++) {
				
				
				//Strip Record and get info required to upsert
				JSONObject aRecordObj = hitsArray.getJSONObject(i);
				JSONObject sourceObj = aRecordObj.getJSONObject("_source");
				JSONObject gdmiPayload = sourceObj.getJSONObject("gdmi");
				
				String ogNatID = gdmiPayload.getString("nationalIdentifier");
				
				String indexType = aRecordObj.getString("_type");
				String indexName = aRecordObj.getString("_index");
				String id = aRecordObj.getString("_id");
				currentCount++;
//				System.out.println("hit " + (currentCount) + " : "
//						+ " type : " + indexType
//						+ " indexName : " + indexName
//						+ " id : " + id
//						+ " natID : " + ogNatID
//						
//				);
				
				
				//Normalizing natID
				String normNatID = PUNCTUATION.matcher(ogNatID).replaceAll("");
				normNatID=WHITE_SPACES.matcher(normNatID).replaceAll("");
				normNatID = StringUtils.upperCase(normNatID);
				
				//Insert narmNatID into source
				gdmiPayload.put("nationalIdentifier", normNatID);
				sourceObj.put("gdmi", gdmiPayload);
				
				IndexRequest indexRequest = new IndexRequest(
						indexName,
						indexType,
						id);
				
				indexRequest.source(sourceObj.toMap());
				bulkProcessor.add(indexRequest);
			}
			
			
			
			
			//retrieve next set of records
			ScrollResponse scanAndScrollResponseObj = esPostCallObj.makeScanCall(scrollId,"5");
			//repopulate Hits array
			hitsArray = scanAndScrollResponseObj.getSearchResults().getJSONArray("hits");
			//Update scrollID
			scrollId=scanAndScrollResponseObj.getScrollID();
			System.out.println("Processed " + (currentCount-1) + " records.");
		}
		
		//Close bulkProcessor
		bulkProcessor.awaitClose(200, TimeUnit.SECONDS);
		
		System.out.println("Total Records Processed : " + (currentCount-1));
		
	}
}



