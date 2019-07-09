package org.helper.app;

import java.io.IOException;
import java.util.ArrayList;
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
import org.helper.elasticsearch.model.SearchResponse;
import org.helper.solr.SolrHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

/**
 * Query ES cluster and update nationalIdentifier of the records that match
 *
 */
public class App 
{
	public static PostCalls esPostCallObj = new PostCalls();
	public static SolrHelper solrHelper = new SolrHelper();
	public static final Pattern PUNCTUATION = Pattern.compile("[^0-9\\s\\p{L}]");
	public static final Pattern WHITE_SPACES = Pattern.compile("\\s");
	
	public static void main( String[] args )
	{
		copyESIndexes();
//		copyVolOpFromSolr();
	}


	private static void copyESIndexes() {

		//Elasticsearch server Settings for Upsert
		String fromESServerName = "specialdeves.makanaplatform.com";

		ArrayList<String> indexNamesList = new ArrayList<>();
		indexNamesList.add("qa2.gs-campaign-en");
		indexNamesList.add("qa2.gs-impactfund-en");
		indexNamesList.add("qa2.gs-npopage-en");
		indexNamesList.add("qa2.gs-story-en");

		String toESServerName = "localhost";
		String clusterName = "DarwinESCluster_DEV";
		Integer esPortNum = 9200;
		Integer totalDocuments = 2000;
		Integer bulkSize = 1000;


		for(String eachIndex : indexNamesList){
			System.out.println( "Running ES Cluster Index Copier for : " + eachIndex);
			String esURL = "https://"+ fromESServerName + ":9200/";

			Integer hitsSize = 500;


			ESClient esClientObj = new ESClient(
					toESServerName,
					esPortNum,
					clusterName,
					bulkSize);

//		String query = "{\"size\":"+ hitsSize + ",\"query\":{\"bool\":{\"must\":[{\"term\":{\"gdmi.countryName\":\"" + country + "\"}},{\"term\":{\"gdmi.nationalIdentifierTypeCode\":\""+ natIDTypeCode+ "\"}}]}}}";
			String query = "{\n\t\"size\": " + hitsSize + ",\n    \"query\": {\n        \"match_all\": {}\n    }\n}";
			try {
				ScrollResponse scrollResponse = esPostCallObj.makeScrollCall(query,
						"5",
						esURL + eachIndex); //search/scroll needs the index name

				processScanAndScroll(scrollResponse,
						esClientObj.getBulkProcessor(),
						esClientObj,
						totalDocuments,
						esURL);

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
	}

	private static void copyVolOpFromSolr(){
		String toESServerName = "localhost";
		String clusterName = "DarwinESCluster_DEV";
		String indexName = "qa2.gs-story-en";
		Integer esPortNum = 9200;
		Integer bulkSize = 1000;

		System.out.println( "Running ES Cluster Index Copier for : VolOp from Solr");

		Integer totalDocuments = 1000;


		ESClient esClientObj = new ESClient(
				toESServerName,
				esPortNum,
				clusterName,
				bulkSize);

		try {
			SearchResponse searchResponse = solrHelper.getSolrDocuments(totalDocuments);

			processSolrResponse(searchResponse, esClientObj.getBulkProcessor(), indexName, esClientObj);


			RestHighLevelClient esClient = esClientObj.getEsClient();

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
	
	private static void processSearchResponse(SearchResponse esSearchResponse,
											  BulkProcessor bulkProcessor,
											  ESClient esClientObj) throws IOException, InterruptedException {
		
		//find total hits, we need to process this many records
		Integer totalHitsCount = esSearchResponse.getTotalHitsCount();
		System.out.println("totalHitsCount : " + totalHitsCount);
		//set start record counter to 1
		Integer currentCount = 1;
		
		//first set of results with Scroll call
		JSONObject searchResults = esSearchResponse.getSearchResults();
		JSONArray hitsArray = searchResults.getJSONArray("hits");

		try {
			for(int i=0;i<hitsArray.length();i++) {


				//Strip Record and get info required to upsert
				JSONObject aRecordObj = hitsArray.getJSONObject(i);
				JSONObject sourceObj = aRecordObj.getJSONObject("_source");
				String indexType = aRecordObj.getString("_type");
				String indexName = aRecordObj.getString("_index");
				String id = aRecordObj.getString("_id");
				currentCount++;
//				System.out.println("hit " + (currentCount) + " : "
//						+ " type : " + indexType
//						+ " indexName : " + indexName
//						+ " id : " + id
//						+ " record : " + aRecordObj.toString()
//				);


				//Insert record into local ES instance
				IndexRequest indexRequest = new IndexRequest(
						indexName,
						indexType,
						id);

				indexRequest.source(sourceObj.toMap());
				bulkProcessor.add(indexRequest);
			}
		} catch (JSONException e) {
      		System.out.println("Caught Error while processing search response " + e.getStackTrace());
		}

		
		//Close bulkProcessor
		bulkProcessor.awaitClose(200, TimeUnit.SECONDS);
		
		System.out.println("Total Records Processed : " + (currentCount-1));
		
	}

	private static void processScanAndScroll(ScrollResponse scrollResponseObj,
											 BulkProcessor bulkProcessor,
											 ESClient esClientObj,
											 Integer totalDocs,
											 String esURL
	) throws IOException, InterruptedException {

		//totalDocs, we need to process this many records
		System.out.println("totalDocs : " + totalDocs);

		//set start record counter to 1
		Integer currentCount = 1;
		Integer oldRecordCount = currentCount;
		//first set of results with Scroll call
		JSONObject scrollResults = scrollResponseObj.getSearchResults();
		JSONArray hitsArray = scrollResults.getJSONArray("hits");
		String scrollId =scrollResponseObj.getScrollID();


		while(currentCount<=totalDocs) {
			for(int i=0;i<hitsArray.length();i++) {
				if(currentCount>totalDocs){
					break;
				}
				//Strip Record and get info required to upsert
				JSONObject aRecordObj = hitsArray.getJSONObject(i);
//				if(!aRecordObj.toString().toLowerCase().contains("test")) {
					JSONObject sourceObj = aRecordObj.getJSONObject("_source");
					String indexType = aRecordObj.getString("_type");
					String indexName = aRecordObj.getString("_index");
					String id = aRecordObj.getString("_id");
					currentCount++;
	//				System.out.println("hit " + (currentCount) + " : "
	//						+ " type : " + indexType
	//						+ " indexName : " + indexName
	//						+ " id : " + id
	//						+ " record : " + aRecordObj.toString()
	//				);


					//Insert record into local ES instance
					IndexRequest indexRequest = new IndexRequest(
							indexName,
							indexType,
							id);

					indexRequest.source(sourceObj.toMap());
					bulkProcessor.add(indexRequest);
//				}
			}

			//retrieve next set of records
			ScrollResponse scanAndScrollResponseObj = esPostCallObj.makeScanCall(scrollId,"5", esURL);
			//repopulate Hits array
			hitsArray = scanAndScrollResponseObj.getSearchResults().getJSONArray("hits");
			//Update scrollID
			scrollId=scanAndScrollResponseObj.getScrollID();
			System.out.println("Processed " + (currentCount-1) + " records.");
			if(currentCount > oldRecordCount){
				oldRecordCount = currentCount;
			}else {
				break; // avoid infinite loop
			}
		}

		//Close bulkProcessor
		bulkProcessor.awaitClose(200, TimeUnit.SECONDS);

		System.out.println("Total Records Processed : " + (currentCount-1));

	}

	private static void processSolrResponse(SearchResponse esSearchResponse,
											  BulkProcessor bulkProcessor,
											  String indexName,
											  ESClient esClientObj) throws IOException, InterruptedException {

		//find total hits, we need to process this many records
		Integer totalHitsCount = esSearchResponse.getTotalHitsCount();
		System.out.println("totalHitsCount : " + totalHitsCount);
		//set start record counter to 1
		Integer currentCount = 1;

		//first set of results with Scroll call
		JSONObject searchResults = esSearchResponse.getSearchResults();
		JSONArray hitsArray = searchResults.getJSONArray("solrReponse");

		try {
			for(int i=0;i<hitsArray.length();i++) {


				//Strip Record and get info required to upsert
				JSONObject sourceObj = hitsArray.getJSONObject(i);
//				JSONObject sourceObj = aRecordObj.getJSONObject("_source");
				String indexType = "_doc";
				currentCount++;
//				System.out.println("hit " + (currentCount) + " : "
//						+ " type : " + indexType
//						+ " indexName : " + indexName
//						+ " id : " + id
//						+ " record : " + aRecordObj.toString()
//				);


				//Insert record into local ES instance
				IndexRequest indexRequest = new IndexRequest(
						indexName,
						indexType);

				indexRequest.source(sourceObj.toMap());
				bulkProcessor.add(indexRequest);
			}
		} catch (JSONException e) {
			System.out.println("Caught Error while processing search response " + e.getStackTrace());
		}


		//Close bulkProcessor
		bulkProcessor.awaitClose(200, TimeUnit.SECONDS);

		System.out.println("Total Records Processed : " + (currentCount-1));

	}
}



