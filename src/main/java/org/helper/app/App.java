package org.helper.app;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import org.helper.app.helpers.RandomDates;
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

/** Query ES cluster and update nationalIdentifier of the records that match */
public class App {
  public static PostCalls esPostCallObj = new PostCalls();
  public static SolrHelper solrHelper = new SolrHelper();
  public static final Pattern PUNCTUATION = Pattern.compile("[^0-9\\s\\p{L}]");
  public static final Pattern WHITE_SPACES = Pattern.compile("\\s");
  public static final ArrayList<String> randomContentNamePool = new ArrayList<>();
  public static Random rand = new Random();
  public static ArrayList<String> trueFalseList =
      new ArrayList<>(
          Arrays.asList(
              "true", "true", "true", "true", "true", "true", "true", "true", "true", "false"));
  public static ArrayList<String> volOpTypesList =
      new ArrayList<>(Arrays.asList("One Time", "Shifts", "Flexible", "Recurring", "Virtual"));

  public static ArrayList<String> daysOfTheWeek =
      new ArrayList<>(
          Arrays.asList(
              "Monday",
              "Tuesday",
              "Wednesday",
              "Thursday",
              "Thursday",
              "Friday",
              "Saturday",
              "Sunday"));

  public static ArrayList<String> timesOfTheDay =
      new ArrayList<>(Arrays.asList("Morning", "Afternoon", "Evening"));

//  public static ArrayList<String> preferredSkillsList =
//      new ArrayList<>(
//          Arrays.asList(
//              "Active Listening",
//              "Communication",
//              "Computer Skills",
//              "Customer Service",
//              "Management Skills",
//              "Problem-Solving",
//              "Time Management",
//              "Transferable Skills",
//              "Self-motivation",
//              "Adaptability",
//              "Decision Making"));

  public static ArrayList<Long> preferredSkillsList =
          new ArrayList<>(
                  Arrays.asList(
                          1000L, //"Active Listening",
                          1001L, //"Communication",
                          1002L, //"Computer Skills",
                          1003L, //"Customer Service",
                          1004L, //"Management Skills",
                          1005L, //"Problem-Solving",
                          1006L, //"Time Management",
                          1007L, //"Transferable Skills",
                          1008L, //"Self-motivation",
                          1009L, //"Adaptability",
                          1010L)); //Decision Making"));

  public static void main(String[] args) {
    Integer totalESDocuments = 10000;
    Integer totalSolrDocuments = 1000;
    copyESIndexes(totalESDocuments);
    copyVolOpFromSolr(totalSolrDocuments);
  }

  private static void copyESIndexes(Integer totalDocuments) {

    // Elasticsearch server Settings for Upsert
    String fromESServerName = "specialdeves.makanaplatform.com";

    ArrayList<String> indexNamesList = new ArrayList<>();
    indexNamesList.add("qa.gs-npopage-en");
    //    indexNamesList.add("qa.gs-campaign-en");
    //    indexNamesList.add("qa.gs-impactfund-en");
    //    indexNamesList.add("qa.gs-story-en");

    //		String toESServerName = "localhost";
    String toESServerName = "developeres.makanaplatform.com";

    String clusterName = "developer";
    Integer esPortNum = 9200;
    Integer bulkSize = 1000;

    for (String eachIndex : indexNamesList) {
      System.out.println("Running ES Cluster Index Copier for : " + eachIndex);
      String esURL = "https://" + fromESServerName + ":9200/";

      Integer hitsSize = 1000;

      ESClient esClientObj = new ESClient(toESServerName, esPortNum, clusterName, bulkSize);

      //		String query = "{\"size\":"+ hitsSize +
      // ",\"query\":{\"bool\":{\"must\":[{\"term\":{\"gdmi.countryName\":\"" + country +
      // "\"}},{\"term\":{\"gdmi.nationalIdentifierTypeCode\":\""+ natIDTypeCode+ "\"}}]}}}";
      String query =
          "{\n\t\"size\": " + hitsSize + ",\n    \"query\": {\n        \"match_all\": {}\n    }\n}";
      try {
        String toESIndexName = eachIndex.replace("qa", "rohitachar");
        toESIndexName = toESIndexName.replace("gs", "v2.gs");
        ScrollResponse scrollResponse =
            esPostCallObj.makeScrollCall(
                query, "5", esURL + eachIndex); // search/scroll needs the index name

        processScanAndScroll(
            scrollResponse,
            esClientObj.getBulkProcessor(),
            esClientObj,
            totalDocuments,
            esURL,
            toESIndexName);

        RestHighLevelClient esClient = esClientObj.getEsClient();

        if (esClient != null) {
          System.out.println("Closing ES Client");
          esClient.close();
        }

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

  private static void copyVolOpFromSolr(Integer totalDocuments) {
    String toESServerName = "specialdeves.makanaplatform.com";
    String clusterName = "DarwinESCluster_DEV";
    String indexName = "search2.globalsearch-volunteering-fr";
    Integer esPortNum = 9200;
    Integer bulkSize = 1000;

    System.out.println("Running ES Cluster Index Copier for : VolOp from Solr");

    ESClient esClientObj = new ESClient(toESServerName, esPortNum, clusterName, bulkSize);

    try {
      solrHelper.getSolrDocuments(
              totalDocuments,
              esClientObj.getBulkProcessor(),
              indexName);



      RestHighLevelClient esClient = esClientObj.getEsClient();

      if (esClient != null) {
        System.out.println("Closing ES Client");
        esClient.close();
      }

      System.out.println("Finished Running Record Updater!");
    } catch (IOException e) {
      System.out.println("Encountered IOException :" + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void processSearchResponse(
      SearchResponse esSearchResponse, BulkProcessor bulkProcessor, ESClient esClientObj)
      throws IOException, InterruptedException {

    // find total hits, we need to process this many records
    Integer totalHitsCount = esSearchResponse.getTotalHitsCount();
    System.out.println("totalHitsCount : " + totalHitsCount);
    // set start record counter to 1
    Integer currentCount = 1;

    // first set of results with Scroll call
    JSONObject searchResults = esSearchResponse.getSearchResults();
    JSONArray hitsArray = searchResults.getJSONArray("hits");

    try {
      for (int i = 0; i < hitsArray.length(); i++) {

        // Strip Record and get info required to upsert
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

        // Insert record into local ES instance
        IndexRequest indexRequest = new IndexRequest(indexName, indexType, id);

        indexRequest.source(sourceObj.toMap());
        bulkProcessor.add(indexRequest);
      }
    } catch (JSONException e) {
      System.out.println("Caught Error while processing search response " + e.getMessage());
    }

    // Close bulkProcessor
    bulkProcessor.awaitClose(200, TimeUnit.SECONDS);

    System.out.println("Total Records Processed : " + (currentCount - 1));
  }

  private static void processScanAndScroll(
      ScrollResponse scrollResponseObj,
      BulkProcessor bulkProcessor,
      ESClient esClientObj,
      Integer totalDocs,
      String esURL,
      String indexName)
      throws IOException, InterruptedException {

    // totalDocs, we need to process this many records
    System.out.println("totalDocs : " + totalDocs);

    // set start record counter to 1
    Integer currentCount = 1;
    Integer oldRecordCount = currentCount;
    // first set of results with Scroll call
    JSONObject scrollResults = scrollResponseObj.getSearchResults();
    JSONArray hitsArray = scrollResults.getJSONArray("hits");
    String scrollId = scrollResponseObj.getScrollID();

    while (currentCount <= totalDocs) {
      for (int i = 0; i < hitsArray.length(); i++) {
        if (currentCount > totalDocs) {
          break;
        }
        // Strip Record and get info required to upsert
        JSONObject aRecordObj = hitsArray.getJSONObject(i);

        JSONObject sourceObj = aRecordObj.getJSONObject("_source");
        String sourceAsString = sourceObj.toString();
        if (sourceAsString.toLowerCase().contains("test")
            || sourceAsString.toLowerCase().contains("npopage_")) {
          continue;
        }

        String indexType = aRecordObj.getString("_type");
        //					String indexName = aRecordObj.getString("_index");
        String id = aRecordObj.getString("_id");
        currentCount++;
        //				System.out.println("hit " + (currentCount) + " : "
        //						+ " type : " + indexType
        //						+ " indexName : " + indexName
        //						+ " id : " + id
        //						+ " record : " + aRecordObj.toString()
        //				);

        // Populate fields needed for search use cases
        sourceObj = populateMissingFields(sourceObj, indexName);

        // Insert record into local ES instance
        IndexRequest indexRequest = new IndexRequest(indexName, indexType, id);

        indexRequest.source(sourceObj.toMap());
        bulkProcessor.add(indexRequest);
        //				}
      }

      // retrieve next set of records
      ScrollResponse scanAndScrollResponseObj = esPostCallObj.makeScanCall(scrollId, "5", esURL);
      // repopulate Hits array
      hitsArray = scanAndScrollResponseObj.getSearchResults().getJSONArray("hits");
      // Update scrollID
      scrollId = scanAndScrollResponseObj.getScrollID();
      System.out.println("Processed " + (currentCount - 1) + " records.");
      if (currentCount > oldRecordCount) {
        oldRecordCount = currentCount;
      } else {
        break; // avoid infinite loop
      }
    }

    // Close bulkProcessor
    bulkProcessor.awaitClose(200, TimeUnit.SECONDS);

    System.out.println("Total Records Processed : " + (currentCount - 1));
  }

  public static JSONObject populateMissingFields(JSONObject sourceObj, String indexName) {
    try {
      {
        //copy city and state to _source payload
        if(sourceObj.has("ui")){
          JSONObject uiObject = sourceObj.getJSONObject("ui");
          if(uiObject.has("city")){
            sourceObj.put("city", uiObject.getString("city"));
          }
          if(uiObject.has("state")){
            sourceObj.put("state", uiObject.getString("state"));
          }
        }

        // Add related_content_name & aka_name
        if (indexName.contains("npopage") && sourceObj.has("name")) {
          JSONArray nameArray = sourceObj.getJSONArray("name");
          for (int j = 0; (j < nameArray.length() && randomContentNamePool.size() < 10000); j++) {
            randomContentNamePool.add(nameArray.getString(j));
          }
        }

        if (sourceObj.has("name") && sourceObj.has("hint") && !indexName.contains("volunteering")) {
          String akaName;
          // get first string token from name and combine it with non Goal cause name
          //      		System.out.println("sourceObj.getJSONArray(\"name\") = " +
          // sourceObj.getJSONArray("name"));
          String nameToken = sourceObj.getJSONArray("name").getString(0).split(" ")[0];

          String causeName = null;
          for (int i = 0; i < sourceObj.getJSONArray("hint").length(); i++) {
            if (!sourceObj.getJSONArray("hint").getString(i).contains("Goal")) {
              causeName = sourceObj.getJSONArray("hint").getString(i);
            }
          }
          if (causeName != null) {
            akaName = nameToken + " " + causeName;
          } else {
            akaName = nameToken;
          }
          sourceObj.put("aka_name", akaName);
        }

        if (!sourceObj.has("related_content_name") && randomContentNamePool.size() > 0) {
          int counter = 3;
          HashSet<String> relatedContentNamesSet = new HashSet<>();
          while (counter > 0) {
            relatedContentNamesSet.add(
                    randomContentNamePool.get(rand.nextInt(randomContentNamePool.size())));

            counter--;
          }
          sourceObj.put("related_content_name", relatedContentNamesSet.toArray());
        }

        sourceObj.put("active_status", trueFalseList.get(rand.nextInt(trueFalseList.size())));

        // add content specific data

        // Campaign
        if (indexName.contains("campaign")) {
          sourceObj.put(
                  "campaign.has_donations", trueFalseList.get(rand.nextInt(trueFalseList.size())));
          sourceObj.put("has_volOp", trueFalseList.get(rand.nextInt(trueFalseList.size())));
          sourceObj.put("volOp_owner", 10000 - rand.nextInt(9000));
          sourceObj.put("goal", rand.nextInt(100) * 10000);
          sourceObj.put("progress_towards_goal", rand.nextInt(100));
        }

        if (indexName.contains("volunteering")) {

          try {
            if(sourceObj.has("description")){
              sourceObj.put("body", sourceObj.getString("description").replaceAll("\\n", ""));
            }
          } catch (Exception e) {
            System.out.println("Error while processing volOp record : " + sourceObj.toString());
          }


          if(sourceObj.has("sdg")){
            sourceObj.put("hint", sourceObj.getJSONArray("sdg"));
          }

          if(sourceObj.has("aggregatefield")){
            if(sourceObj.has("description")){
              String descriptionString = sourceObj.getString("description");
              JSONArray akaNameJsonArray = sourceObj.getJSONArray("aggregatefield");
              JSONArray finalAkaNameJsonArray = new JSONArray();
              for(int i =0; i<akaNameJsonArray.length();i++){
                if(!akaNameJsonArray.getString(i).equalsIgnoreCase(descriptionString)){
                  finalAkaNameJsonArray.put(akaNameJsonArray.get(i));
                }
              }
              sourceObj.put("aka_name", finalAkaNameJsonArray);
            }else{
              sourceObj.put("aka_name", sourceObj.getJSONArray("aggregatefield"));
            }

          }
          sourceObj.put("type", "volunteering");
          sourceObj.put("goal", rand.nextInt(100) * 10000);
          sourceObj.put("progress_towards_goal", rand.nextInt(100));

          sourceObj.put(
                  "volOp_types", volOpTypesList.get(rand.nextInt(volOpTypesList.size())));

          if (sourceObj.has("active_status")
                  && sourceObj.getString("active_status").equalsIgnoreCase("true")) {
            sourceObj.put("start_date", RandomDates.createRandomDate(2017, 2018));
            sourceObj.put("end_date", RandomDates.createRandomDate(2020, 2021));
          } else {
            sourceObj.put("start_date", RandomDates.createRandomDate(2015, 2016));
            sourceObj.put("end_date", RandomDates.createRandomDate(2017, 2018));
          }

          HashSet<String> volOpAvailableDaysList = new HashSet<>();
          Integer maxDaysOfTheWeek = rand.nextInt(6);
          for (int i = 0; i <= maxDaysOfTheWeek; i++) { // add days
            volOpAvailableDaysList.add(daysOfTheWeek.get(rand.nextInt(daysOfTheWeek.size())));
          }
          sourceObj.put("available_days", volOpAvailableDaysList.toArray());

          HashSet<String> volOpAvailableTimesList = new HashSet<>();
          Integer maxAvailableTimes = rand.nextInt(2);
          for (int i = 0; i <= maxAvailableTimes; i++) { // add times of day
            volOpAvailableTimesList.add(timesOfTheDay.get(rand.nextInt(timesOfTheDay.size())));
          }
          sourceObj.put("available_time", volOpAvailableTimesList.toArray());

          HashSet<Long> volOpPreferredSkillsList = new HashSet<>();
          Integer maxPreferredSkills = rand.nextInt(5);
          for (int i = 0; i <= maxPreferredSkills; i++) { // add times of day
            volOpPreferredSkillsList.add(
                    preferredSkillsList.get(rand.nextInt(preferredSkillsList.size())));
          }
          sourceObj.put("preferred_skills", volOpPreferredSkillsList.toArray());
        }

        if (indexName.contains("story")) {
          if (sourceObj.has("active_status")
                  && sourceObj.getString("active_status").equalsIgnoreCase("true")) {
            sourceObj.put("start_date", RandomDates.createRandomDate(2017, 2018));
            sourceObj.put("end_date", RandomDates.createRandomDate(2020, 2021));
          } else {
            sourceObj.put("start_date", RandomDates.createRandomDate(2015, 2016));
            sourceObj.put("end_date", RandomDates.createRandomDate(2017, 2018));
          }
        }

        return sourceObj;
      }
    } catch (Exception e) {
      System.out.println("Error while creating content payload : " + e.getMessage());
      System.out.println("Json Payload " + sourceObj.toString());
      e.printStackTrace();
      return sourceObj;
    }
  }

}
