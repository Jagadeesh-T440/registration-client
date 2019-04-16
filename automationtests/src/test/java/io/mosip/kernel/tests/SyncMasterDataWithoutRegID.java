package io.mosip.kernel.tests;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.base.Verify;
import com.jayway.jsonpath.internal.filter.ValueNode.JsonNode;

import io.mosip.dbaccess.KernelMasterDataR;
import io.mosip.dbentity.BlacklistedWords;
import io.mosip.dbentity.UinEntity;
import io.mosip.service.ApplicationLibrary;
import io.mosip.service.AssertKernel;
import io.mosip.service.BaseTestCase;
import io.mosip.util.ReadFolder;
import io.mosip.util.ResponseRequestMapper;
import io.restassured.response.Response;

/**
 * @author M9010714
 *
 */
public class SyncMasterDataWithoutRegID extends BaseTestCase implements ITest {

	public SyncMasterDataWithoutRegID() {
		// TODO Auto-generated constructor stub
		super();
	}
	/**
	 *  Declaration of all variables
	 */
	private static Logger logger = Logger.getLogger(SyncMasterDataWithoutRegID.class);
	protected static String testCaseName = "";
	static SoftAssert softAssert=new SoftAssert();
	public static JSONArray arr = new JSONArray();
	boolean status = false;
	private static ApplicationLibrary applicationLibrary = new ApplicationLibrary();
	private static AssertKernel assertKernel = new AssertKernel();
	private static final String fetchmasterdata = "/v1/syncdata/masterdata";
	public KernelMasterDataR dbConnection=new KernelMasterDataR();
	static String dest = "";
	static String folderPath = "kernel/SyncMasterDataWithoutRegID";
	static String outputFile = "SyncMasterDataWithoutRegIDOutput.json";
	static String requestKeyFile = "SyncMasterDataWithoutRegIDInput.json";
	static JSONObject Expectedresponse = null;
	String finalStatus = "";
	static String testParam="";
	/*
	 * Data Providers to read the input json files from the folders
	 */
	@BeforeMethod
	public static void getTestCaseName(Method method, Object[] testdata, ITestContext ctx) throws Exception {
		JSONObject object = (JSONObject) testdata[2];
		// testName.set(object.get("testCaseName").toString());
		testCaseName = object.get("testCaseName").toString();
	} 
	
	/**
	 * @return input jsons folders
	 * @throws Exception
	 */
	@DataProvider(name = "SyncMasterDataWithoutRegID")
	public static Object[][] readData1(ITestContext context) throws Exception {
	
		 testParam = context.getCurrentXmlTest().getParameter("testType");
		switch ("smoke") {
		case "smoke":
			return ReadFolder.readFolders(folderPath, outputFile, requestKeyFile, "smoke");
		case "regression":
			return ReadFolder.readFolders(folderPath, outputFile, requestKeyFile, "regression");
		default:
			return ReadFolder.readFolders(folderPath, outputFile, requestKeyFile, "smokeAndRegression");
		}
	}
	
	
	/**
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 * getAllConfiguration
	 * Given input Json as per defined folders When GET request is sent to /syncdata/v1.0/masterdata/{regcenterId}
	 * Then Response is expected as 200 and other responses as per inputs passed in the request
	 */
	@Test(dataProvider="SyncMasterDataWithoutRegID")
	public void getAllConfiguration(String testSuite, Integer i, JSONObject object) throws FileNotFoundException, IOException, ParseException
    {
	
		JSONObject actualRequest = ResponseRequestMapper.mapRequest(testSuite, object);
		Expectedresponse = ResponseRequestMapper.mapResponse(testSuite, object);
		
		/*
		 * Calling the GET method with both1	 path and query parameter
		 */
		@SuppressWarnings("unchecked")
		Response res=applicationLibrary.getRequestAsQueryParam(fetchmasterdata, actualRequest);
		
		String resultTime = res.jsonPath().get("responsetime").toString();
		String lastSyncTime = res.jsonPath().get("$response.lastSyncTime");
		Expectedresponse.put("responsetime", resultTime);
		Expectedresponse.put("$response.lastSyncTime", lastSyncTime);
		
		/*
		 * Removing the unstable attributes from response	
		 */
		List<String> outerKeys = new ArrayList<String>();
		List<String> innerKeys = new ArrayList<String>();
		
		innerKeys.add("timestamp");	
	 //   outerKeys.add("responsetime");
		//outerKeys.add("licenseKey");
		innerKeys.add("lastSyncTime");
		
		ArrayList<String> listOfElementToRemove=new ArrayList<String>();
		listOfElementToRemove.add("timestamp");
		listOfElementToRemove.add("lastSyncTime");
		listOfElementToRemove.add("responsetime");
		/*
		 * Comparing expected and actual response
		 */
	//	status = AssertResponses.assertResponses(res, Expectedresponse, outerKeys, innerKeys);
		
		status = assertKernel.assertKernel(res, Expectedresponse,listOfElementToRemove);
		
//		String query1="select m.word from master.blacklisted_words m";
		HashMap<String, String> tableCount=new HashMap();
		
		 tableCount=res.jsonPath().get("response");
		List<String> status_list=null;
		Set<String> keys = tableCount.keySet();
		HashMap<String,Class> Dbentity=new HashMap();
		Dbentity.put("blackListedWords", BlacklistedWords.class);
		
		
		
		for(String table:keys)
		{
			String query1="'select m.code from master."+table+" m'";
			Class ent = Dbentity.get("blackListedWords");
			 status_list = dbConnection.getDataFromDB(ent,query1);
			 System.out.println("black------------------------>"+status_list.size());
		}
		
		
		
		
	//	System.out.println("black------------------------>"+status_list.size());
      if (status) {
	            
				finalStatus = "Pass";
			}	
		
		else {
			finalStatus="Fail";
			logger.error(res);
			//softAssert.assertTrue(false);
		}
		
		softAssert.assertAll();
		object.put("status", finalStatus);
		arr.add(object);
		boolean setFinalStatus=false;
		if(finalStatus.equals("Fail"))
			setFinalStatus=false;
		else if(finalStatus.equals("Pass"))
			setFinalStatus=true;
		/*Verify.verify(setFinalStatus);
		softAssert.assertAll();
*/
}
		@Override
		public String getTestName() {
			return this.testCaseName;
		} 
		
		@AfterMethod(alwaysRun = true)
		public void setResultTestName(ITestResult result) {
			
	try {
				Field method = TestResult.class.getDeclaredField("m_method");
				method.setAccessible(true);
				method.set(result, result.getMethod().clone());
				BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
				Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
				f.setAccessible(true);

				f.set(baseTestMethod, SyncMasterDataWithoutRegID.testCaseName);

				
			} catch (Exception e) {
				Reporter.log("Exception : " + e.getMessage());
			}
		}  
		
		@AfterClass
		public void updateOutput() throws IOException {
			String configPath = "src/test/resources/kernel/SyncMasterDataWithoutRegID/SyncMasterDataWithoutRegIDOutput.json";
			try (FileWriter file = new FileWriter(configPath)) {
				file.write(arr.toString());
				logger.info("Successfully updated Results to SyncMasterDataWithoutRegIDOutput.json file.......................!!");
			}
		}
}
