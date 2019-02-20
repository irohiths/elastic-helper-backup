package org.helper.elasticsearch.commands;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class BaseIndexer implements Runnable {
	

	protected List<String>recordsList;	

	public abstract int processAFile(File aFile)throws org.json.JSONException, IOException;
	public abstract char getCSVFileDelimeter();

	protected static Map<String,String>isoCCNumToFIPSCCNameMap;

	protected static String SQL_USER;
	protected static String SQL_PASSWORD;
	protected static String SQL_URL_STR;
	protected static String ES_BASE_PASS_THRU_URL;

	protected String  folder;
	protected File    propsFile;
	protected String  indexName;
	protected String  additionalIndexName;
	protected String  type;
	protected String  country;
	protected String  serverName;
	protected String  clusterName;
	protected Integer portNumber;
	protected String  fileNameIncludePath;
	protected String  typeOfData;
	protected String  startingLine;
	protected String   env;
	protected Integer  bulkSize;

	public static OkHttpClient elasticSerachHttpClient;

	static List<BulkItemResponse>bulkErrorsList;

	//Default constructor should have default access, 
	//and is used only for testing purposes
	public BaseIndexer(){
		//Do nothing
		if(null==BaseIndexer.bulkErrorsList){
			synchronized (this) {
				BaseIndexer.bulkErrorsList=new ArrayList<BulkItemResponse>();	
			}
		}
	}

	public BaseIndexer(String propsFile,
			String env,
			String bulkSize){
		this.propsFile=new File(propsFile);
		this.env=env;
		if(null==BaseIndexer.bulkErrorsList){
			synchronized (this) {
				BaseIndexer.bulkErrorsList=new ArrayList<BulkItemResponse>();	
			}
		}
//		if(null==BaseIndexer.dataSource){
//			try {
//				this.initMySQL();
//			} 
//			catch (IOException e) {
//
//				e.printStackTrace();
//			}
//		}
//		if(null==BaseIndexer.regionsConfigHandler){
//			BaseIndexer.regionsConfigHandler=new RegionsConfigHandler(BaseIndexer.dataSource,this.env);
//			BaseIndexer.regionsConfigHandler.setDataSource(BaseIndexer.dataSource);
//		}		
		if(null==BaseIndexer.isoCCNumToFIPSCCNameMap){
			BaseIndexer.init();
		}
		this.bulkSize=500;
		if(null!=bulkSize){
			bulkSize=bulkSize.trim();
			if(!bulkSize.isEmpty()){
				try{
					this.bulkSize=Integer.parseInt(bulkSize);
				}
				catch(NumberFormatException e){
					this.bulkSize=500;
				}
			}
		}
		else{
			this.bulkSize=500;
		}
	}


	public BaseIndexer(String folder, 
			String propsFile,
			String indexName, 
			String type, 
			String country, 
			String serverName,
			String clusterName,
			String portNumber,
			String fileNameIncludePath,
			String typeOfData,
			String startingLine,
			String env,
			String bulkSize){

		this.folder=folder;
		if(null!=this.folder){
			this.folder=this.folder.trim();
		}
		this.propsFile=null;
		if(null!=propsFile && !propsFile.trim().isEmpty()){
			this.propsFile=new File(propsFile);
		}
		this.indexName = indexName;
		if(null!=this.indexName){
			this.indexName=this.indexName.trim();
		}
		this.type = type;
		if(null!=this.type){
			this.type=this.type.trim();
		}
		this.country = country;
		if(null!=this.country){
			this.country=this.country.trim();
		}
		this.clusterName = clusterName;
		if(null!=this.clusterName){
			this.clusterName=this.clusterName.trim();
		}
		this.serverName=serverName;
		if(null!=this.serverName){
			this.serverName=this.serverName.trim();
		}
		if(null!=portNumber && !portNumber.trim().isEmpty()){
			try{
				this.portNumber=Integer.valueOf(portNumber);
			}
			catch(NumberFormatException e){
				System.out.println("ERROR: in BaseIndexer Construtor, portNumber is invalid, portNumber="+
						portNumber+" using default portNumber: 9200");
				this.portNumber=9200;
			}
		}
		this.fileNameIncludePath = fileNameIncludePath;
		if(null!=this.fileNameIncludePath){
			this.fileNameIncludePath=this.fileNameIncludePath.trim();
		}
		this.typeOfData = typeOfData;
		if(null!=this.typeOfData){
			this.typeOfData=this.typeOfData.trim();
		}
		this.startingLine = startingLine;
		if(null!=this.startingLine){
			this.startingLine=this.startingLine.trim();
		}
		this.env=env;
//		if(null==BaseIndexer.dataSource){
//			try {
//				this.initMySQL();
//			} 
//			catch (IOException e) {
//
//				e.printStackTrace();
//			}
//		}
//		if(null==BaseIndexer.regionsConfigHandler){
//			BaseIndexer.regionsConfigHandler=new RegionsConfigHandler(BaseIndexer.dataSource,this.env);
//			BaseIndexer.regionsConfigHandler.setDataSource(BaseIndexer.dataSource);
//		}
		if(null==BaseIndexer.isoCCNumToFIPSCCNameMap){
			BaseIndexer.init();
		}
		this.bulkSize=500;
		if(null!=bulkSize){
			bulkSize=bulkSize.trim();
			if(!bulkSize.isEmpty()){
				try{
					this.bulkSize=Integer.parseInt(bulkSize);
				}
				catch(NumberFormatException e){
					this.bulkSize=500;
				}
			}
		}
		else{
			this.bulkSize=500;
		}
	}





	@Override
	public void run(){
		if(null!=this.recordsList){
//			try{
//				this.processRecords();
//			}
//			catch(JSONException e){
//				e.printStackTrace();
//			}
//			catch(IOException e){
//				e.printStackTrace();
//			}
		}
	}


	public RestHighLevelClient getEsClient(){
		return BaseIndexer.getElasticServerClient(this.serverName, 
				this.portNumber, 
				this.clusterName);
	}
	//give this default access
	void initMySQL()throws IOException{
		String METHOD_NAME = "initMySQL()";

//		Properties modernMatchPropFile=new Properties();
//		//		InputStream inny=new FileInputStream(new File("/Users/doanth/Downloads/Programming/Eclipse/modern-match-service/src/main/resources/modern-match-config.properties"));
//		InputStream inny=null;
//		if(null==this.propsFile || !this.propsFile.exists()){
//			inny=BaseIndexer.class.getClassLoader().getResourceAsStream(Constants.SEARCH_PRODUCER_GDMI_CONFIG_FILE_NAME);			
//		}
//		else{
//			inny=new FileInputStream(this.propsFile);
//		}
//		modernMatchPropFile.load(inny);

//		if (null!=sqlUserBase64 && !sqlUserBase64.trim().isEmpty() && null!=sqlPasswordBase64 && !sqlPasswordBase64.isEmpty()){

//			synchronized (BaseIndexer.class) {
//
//				BaseIndexer.SQL_USER=new String(Base64.getDecoder().decode(sqlUserBase64.getBytes()));
//				BaseIndexer.SQL_PASSWORD=new String(Base64.getDecoder().decode(sqlPasswordBase64.getBytes()));				
//				BaseIndexer.SQL_URL_STR=modernMatchPropFile.getProperty(Constants.SQL_URL);
//
//				BaseIndexer.dataSource=new DriverManagerDataSource();
//				BaseIndexer.dataSource.setDriverClassName(Constants.SQL_DRIVER_STR);
//				System.out.println(METHOD_NAME+": sqlUrl="+BaseIndexer.SQL_URL_STR);
//				if(log.isDebugEnabled()){
//					System.out.println((METHOD_NAME+": sqlUrl="+BaseIndexer.SQL_URL_STR));
//				}
//				BaseIndexer.dataSource.setUrl(BaseIndexer.SQL_URL_STR);
//
//				BaseIndexer.dataSource.setUsername(BaseIndexer.SQL_USER);
//				BaseIndexer.dataSource.setPassword(BaseIndexer.SQL_PASSWORD);								
//			}
//		}
//		if (log.isDebugEnabled()) {
//			System.out.println(BaseIndexer.class.getName() + "." + METHOD_NAME + ": END");
//		}	
	}

	public static String getFipsCountryCodeFromISOCountryCode(String isoCountryCode){
		String retVal=null;
		if(null!=isoCountryCode && !isoCountryCode.trim().isEmpty()){
			if(null==BaseIndexer.isoCCNumToFIPSCCNameMap){
				BaseIndexer.init();
			}
			retVal=BaseIndexer.isoCCNumToFIPSCCNameMap.get(isoCountryCode.trim());
		}
		return retVal;
	}

	public static RestHighLevelClient getElasticServerClient(
			String  searchHost, 
			Integer searchPort, 
			String  clusterName) {
		String METHOD_NAME = BaseIndexer.class.getName()+".static getElasticServerClient() ";
		RestHighLevelClient restClient=null;
		if(null==searchPort){
			searchPort=9200;
		}
		try {
			if(null==searchHost || searchHost.trim().isEmpty()){
				restClient=new RestHighLevelClient(RestClient.builder(new HttpHost(InetAddress.getByName("localhost"), searchPort, "http")));
			}
			else{
				restClient= new RestHighLevelClient(RestClient.builder(new HttpHost(InetAddress.getByName(searchHost), searchPort, "http")));
			}
		} 
		catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			System.out.println(METHOD_NAME + ": UnknownHostException: "
					+ e.getMessage());
		}
		return restClient;
	}

	public static BulkProcessor getBulkProcessor(
			RestHighLevelClient client, 
			Integer bulkSize,
			Integer concurrReq) {
		BulkProcessor.Listener listener = new BulkProcessor.Listener() { 
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {

			}

			@Override
			public void afterBulk(long executionId, BulkRequest request,
					BulkResponse response) {
				String METHOD_NAME = BaseIndexer.class.getName()+".afterBulk(1)";
				if (response.hasFailures()) {
					System.out.println(METHOD_NAME + ":ERROR: Bulk Indexing encountered an message:"+
							response.buildFailureMessage().toString()+" executionId=" + executionId+" "+
							" request.estimatedSizeInBytes()="+request.estimatedSizeInBytes()+
							" request.numberOfActions()="+request.numberOfActions());
					List<Object>payLoads=request.payloads();
					if(null!=payLoads){
						for(Object aPayload:payLoads){
							System.out.print("~~~~~~~="+aPayload);
						}
					}				
					System.out.println(METHOD_NAME+ ": ERROR: response.buildFailureMessage()=" + 
							response.buildFailureMessage()+
							": executionId=" + executionId);

					BulkItemResponse[]errorsList=response.getItems();
					int errorSize=errorsList.length;
					System.out.println("@@@@@@@@@@@@@@@@bulk errorSize="+errorSize);
					if(errorsList.length>0){
						BulkItemResponse aBulkResponse=errorsList[0];
						System.out.println("@@@@@@@@@@@@@@@@first aBulkResponse.getFailureMessage()="+
								aBulkResponse.getFailureMessage()+
								" aBulkResponse.getIndex()="+aBulkResponse.getIndex());
					}
					//					for(BulkItemResponse anErrorResponse:errorsList){
					//						Failure failure=anErrorResponse.getFailure();
					//						failure.getMessage();
					//						
					//					}
				}
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
				String METHOD_NAME = BaseIndexer.class.getName()+".afterBulk(2)";
				if(null!=failure){
					System.out.println(METHOD_NAME + ":ERROR: Bulk Indexing encountered an message:"+
							failure.getMessage()+" executionId=" + executionId+" "+failure.getMessage()+
							" request.estimatedSizeInBytes()="+request.estimatedSizeInBytes()+
							" request.numberOfActions()="+request.numberOfActions());
					List<Object>payLoads=request.payloads();
					if(null!=payLoads){
						for(Object aPayload:payLoads){
							System.out.print("~~~~~~~="+aPayload);
						}
					}
					System.out.println(METHOD_NAME + ":ERROR: Bulk Indexing encountered an message:"+
							failure.getMessage()+" executionId=" + executionId+" "+failure.getMessage()+" request.estimatedSizeInBytes()="+request.estimatedSizeInBytes()+" request.numberOfActions()="+request.numberOfActions());
				}
			}
		};
		if(null==bulkSize){
			bulkSize=500;
		}
		BulkProcessor bulkProcessor =
				BulkProcessor.builder(client::bulkAsync, listener)
				.setBulkActions(bulkSize)
				.setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB))
				.setFlushInterval(TimeValue.timeValueSeconds(5))
				.setConcurrentRequests(concurrReq)
				.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100),3))
				.build();
		return bulkProcessor;
	}

	public static BulkProcessor getBulkProcessor(
			RestHighLevelClient client, 
			Integer bulkSize) {

		return BaseIndexer.getBulkProcessor(
				client, 
				bulkSize,
				1);
	}

	public RestCallReturnObj makePostCall(String url, String jsonBodyStr) throws IOException {
		String methodName = this.getClass().getName() + ".makePostCall():";
		//		if (log.isDebugEnabled()) {
		//			System.out.println(methodName + Constants.START_STR);
		//
		//		}
		RestCallReturnObj retVal = new RestCallReturnObj();

		RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBodyStr);

		if(null==BaseIndexer.elasticSerachHttpClient) {
			BaseIndexer.elasticSerachHttpClient = new OkHttpClient();
		} 
		Request.Builder builder = new Request.Builder();
		Request elasticSearchReq = builder.url(url).header("Accept", "application/json").post(body).build();

		// Make the elasticsearch call
		long elasticSearchQueryStartTime = System.currentTimeMillis();
		Response theResp;
		try {
			theResp = elasticSerachHttpClient.newCall(elasticSearchReq).execute();

			long elasticSearchQueryStartEnd = System.currentTimeMillis();
			ResponseBody theRespBody = theResp.body();

			retVal.setCallTook(elasticSearchQueryStartEnd - elasticSearchQueryStartTime);
			retVal.setHeaders(theResp.headers());

			retVal.setResponseBody(theRespBody);
		} catch (Exception e) {
			System.out.println(e);
		}
		//		if (log.isDebugEnabled()) {
		//			System.out.println(methodName + "END");
		//			System.out.println("start" + retVal);
		//		}
		return retVal;
	}

	//TODO this is written for ES 2.4.1 we will have to change this in ES6
	public static RestCallReturnObj indexDocWithRESTCommand(
			String url, 
			String jsonBodyStr, 
			boolean isPUT) throws IOException {
		String methodName = BaseIndexer.class.getName() + ".indexDocWithPUTCommand():";
		//		if (log.isDebugEnabled()) {
		//			System.out.println(methodName + Constants.START_STR);
		//
		//		}
		RestCallReturnObj retVal = new RestCallReturnObj();

		RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBodyStr);

		if(null==BaseIndexer.elasticSerachHttpClient) {
			BaseIndexer.elasticSerachHttpClient = new OkHttpClient();
		} 
		Request.Builder builder = new Request.Builder();
		Request elasticSearchReq = null;
		if(isPUT){
			elasticSearchReq=builder.url(url).header("Accept", "application/json").put(body).build();
		}
		else{
			elasticSearchReq=builder.url(url).header("Accept", "application/json").post(body).build();
		}
		// Make the elasticsearch call
		long elasticSearchQueryStartTime = System.currentTimeMillis();
		Response theResp;
		try {
			theResp = elasticSerachHttpClient.newCall(elasticSearchReq).execute();

			long elasticSearchQueryStartEnd = System.currentTimeMillis();
			ResponseBody theRespBody = theResp.body();

			retVal.setCallTook(elasticSearchQueryStartEnd - elasticSearchQueryStartTime);
			retVal.setHeaders(theResp.headers());

			retVal.setResponseBody(theRespBody);
		} catch (Exception e) {
			System.out.println("errior: "+e);
		}
		//		if (log.isDebugEnabled()) {
		//			System.out.println(methodName + "END");
		//			System.out.println("start" + retVal);
		//		}
		return retVal;
	}


	//TODO we have to fix this to use the array of of gdmi.indexes
//	public ResponseBody getDunsRecordFromES(String duns)throws IOException{
//		String serverHost=null;
//		if(null==this.serverName || this.serverName.trim().isEmpty()){
//			serverHost="localhost";
//		}
//		else{
//			serverHost=this.serverName.trim();
//		}
//		if(!serverHost.toLowerCase().startsWith("http://")){
//			serverHost=("http://"+serverHost);
//		}
//		if(serverHost.endsWith("/")){
//			serverHost=serverHost.substring(0, (serverHost.length()-1));
//		}
//		String url = serverHost+":9200/search_companies/duns_companies/_search";
//		QueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("gdmi.duns", duns));
//		JSONObject jsonObjectWrapperForQuery = new JSONObject(qb.toString());
//		JSONObject queryObject = new JSONObject();
//		queryObject.put("query", jsonObjectWrapperForQuery);
//
//		RestCallReturnObj elasticSearchResponse = this.makePostCall(url, queryObject.toString());
//		//System.out.println("URL Queried: " + url);
//		ResponseBody body = elasticSearchResponse.getResponseBody();
//		return body;
//	}

	public Integer getStartingLine(){
		if(null!=this.startingLine && !this.startingLine.trim().isEmpty()){
			try{
				return Integer.parseInt(this.startingLine.trim());
			}
			catch(NumberFormatException e){
				return 0;
			}
		}
		else{
			return 0;
		}
	}

	private static void init(){
		//TODO :Move this to properties file
		BaseIndexer.isoCCNumToFIPSCCNameMap=new HashMap<String,String>();
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("4","af");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("04","af");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("004","af");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("248","fi");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("8","al");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("08","al");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("008","al");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("12","ag");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("012","ag");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("16","aq");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("016","aq");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("20","an");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("020","an");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("24","ao");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("024","ao");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("660","av");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("10","ay");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("010","ay");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("28","ac");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("028","ac");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("32","ar");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("032","ar");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("51","am");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("051","am");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("533","aa");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("36","as");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("036","as");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("40","au");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("040","au");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("31","aj");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("031","aj");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("44","bf");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("044","bf");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("48","ba");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("048","ba");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("50","bg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("050","bg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("52","bb");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("052","bb");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("112","bo");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("56","be");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("056","be");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("84","bh");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("084","bh");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("204","bn");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("60","bd");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("060","bd");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("64","bt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("064","bt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("68","bl");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("068","bl");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("70","bk");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("070","bk");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("72","bc");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("072","bc");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("76","br");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("076","br");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("92","vg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("092","vg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("96","bx");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("096","bx");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("100","bu");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("854","uv");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("108","by");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("116","cb");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("120","cm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("124","ca");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("132","cv");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("136","cj");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("140","ct");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("148","cd");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("152","ci");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("156","ch");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("162","kt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("166","ck");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("170","co");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("174","cn");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("180","cg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("178","cf");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("184","cw");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("188","cs");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("384","iv");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("191","hr");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("192","cu");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("196","cy");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("203","ez");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("208","da");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("262","dj");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("212","do");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("214","dr");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("218","ec");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("818","eg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("222","es");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("226","ek");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("232","er");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("233","en");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("231","et");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("238","fk");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("234","fo");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("242","fj");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("246","fi");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("250","fr");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("254","fg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("258","fp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("260","fs");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("266","gb");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("270","ga");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("268","gg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("276","gm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("288","gh");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("292","gi");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("300","gr");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("304","gl");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("308","gj");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("312","gp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("316","gq");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("320","gt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("324","gv");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("624","pu");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("328","gy");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("332","ha");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("336","vt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("340","ho");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("344","hk");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("348","hu");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("352","ic");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("356","in");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("360","id");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("364","ir");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("368","iz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("372","ei");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("376","is");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("380","it");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("388","jm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("392","ja");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("400","jo");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("398","kz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("404","ke");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("296","kr");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("408","kn");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("410","ks");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("414","ku");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("417","kg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("418","la");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("428","lg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("422","le");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("426","lt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("430","li");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("434","ly");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("438","ls");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("440","lh");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("442","lu");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("446","mc");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("807","mk");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("450","ma");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("454","mi");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("458","my");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("462","mv");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("466","ml");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("470","mt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("584","rm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("474","mb");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("478","mr");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("480","mp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("175","mf");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("484","mx");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("583","fm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("498","md");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("492","mn");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("496","mg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("499","mw");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("499","mj");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("500","mh");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("504","mo");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("508","mz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("104","bm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("516","wa");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("520","nr");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("524","np");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("528","nl");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("530","nt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("540","nc");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("554","nz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("558","nu");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("562","ng");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("566","ni");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("570","ne");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("574","nf");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("580","cq");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("578","no");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("512","mu");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("586","pk");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("585","ps");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("275","gz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("275","we");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("591","pm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("598","pp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("600","pa");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("604","pe");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("608","rp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("612","pc");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("616","pl");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("620","po");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("630","rq");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("634","qa");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("638","re");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("642","ro");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("643","rs");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("646","rw");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("654","sh");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("659","sc");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("662","st");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("666","sb");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("670","vc");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("882","ws");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("674","sm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("678","tp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("682","sa");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("686","sg");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("688","ri");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("690","se");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("694","sl");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("702","sn");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("703","lo");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("705","si");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("90","bp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("090","bp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("706","so");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("710","sf");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("728","od");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("724","sp");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("144","ce");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("736","su");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("740","ns");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("744","sv");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("748","wz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("752","sw");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("756","sz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("760","sy");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("158","tw");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("762","ti");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("834","tz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("764","th");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("626","tt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("768","to");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("772","tl");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("776","tn");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("780","td");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("788","ts");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("792","tu");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("795","tx");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("796","tk");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("798","tv");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("800","ug");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("804","up");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("784","ae");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("826","uk");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("840","us");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("858","uy");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("860","uz");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("548","nh");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("336","vt");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("548","ve");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("704","vm");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("92","vi");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("092","vi");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("850","vq");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("876","wf");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("732","wi");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("887","ym");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("894","za");
		BaseIndexer.isoCCNumToFIPSCCNameMap.put("716","zi");
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

	public static String getAcronym(String inputStr) {
		String retVal = inputStr;
		if (null != retVal) {
			retVal = retVal.toLowerCase();
			retVal = retVal.replaceAll("[^a-zA-Z0-9] ", " ");
			String[] retValArray = retVal.split("\\s");
			StringBuilder temp = new StringBuilder("");
			for (String aWord : retValArray) {
				if (null != aWord && !aWord.isEmpty()) {
					temp.append(aWord.charAt(0));
				}
			}
			retVal = temp.toString();
		}
		return retVal;
	}

	public boolean isAStopWord(String str){
		boolean retVal=false;
		if(null!=str){
			retVal=BaseIndexer.companyNameStopWords.contains(str);
		}
		return retVal;
	}
	private static Set<String> companyNameStopWords = new HashSet<String>(Arrays.asList("a", "an", "and", "are", "as",
			"at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such",
			"that", "the", "their", "then", "there", "these", "they", "this", "to", "was", "will", "with",
			"corporation", "corp.", "llc.", "llc", "ltd.", "ltd", "pvt.", "pvt", "private", "limited", "inc.", "inc",
			"incorporated", "holdings", "limited liability corporation", "corp", "Inc.", "Inc", "india private limited",
			"partnership", "co", "de", "ooo", "ltda", "srl", "sci", "de", "jean", "monsieur", "la", "du", "marie",
			"sarl", "madame", "et", "le", "des", "pierre", "les", "michel", // French
			"services", "the", "uk", "management", "company", "solutions", "consulting", // UK
			"county", "factory", "town", "district", "of", "technology", "trade", "committee", "industry", "school", // China
			"india", "enterprises", "industries", "group", "traders", // India
			"di", "drl", "snc", "sas", "societa", "spa", "immobiliare", // Italy
			"me", "e", "comercio", "do", "da", "s", "servicos", "associacao", "epp", "dos", "cia", "representacoes", // Brazil
			"ooo", "too", "zao", "firma", "ao", "servis", "I", "ichp", "kfkh", "kompaniya", "tsentr", "aozt", "plyus",
			"predstavitelstvo", // Russia
			"gmbh", "und", "kg", "mbh", "r", "dr", "nkt", "haftungsbeschr", "ug", "e'v", "verwaltungs", // Germany
			"z", "sp", "o", "gospodarstwo", "rolne", "firma", "uslugi", "s", "przedsiebiorstwo", "handlowo", "zaklad",
			"mieszkaniowa", // PL
			"sl", "s'l", "y", "sociedad", "extinguida", "sa", "limitada", "c'b", "en", "la", "s", // Spain
			"canada", "ontario", "service", "centre", "enr", // Canada
			"pty", "ltd", "mr", "trust", "investments", "trustee", "family", "australia", "mrs"// Australia
			// TODO add these countries too:
			// k'k,y'k,co,ltd,kogyo,corporation,inc,limited,association,japan,kensetsu,liability,service,shoji,shoten
			// //Japan
			// b'v,van,stichting,de,en,holding,beheer,v'o'f,vereniging,eigenaars,te,der,administratiekantoor,advies,nederland
			// //NL
			// de,y,s'r'l,s'a,s'h,s,del //AR
			// bvba,sprl,de,van,nv,sa,p,asbl,en,a,c,in,vzw,vereffening //BE
			// s'r'o,ing,v,likvidaci,spol,r'o,spolecenstvi //EZ
			// co,ltd,construction,industry,korea,tech,agency,machinery,institute,branch,general,development,clinic,shop
			// //KS
			));

}
