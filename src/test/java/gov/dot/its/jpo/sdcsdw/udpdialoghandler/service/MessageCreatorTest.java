package gov.dot.its.jpo.sdcsdw.udpdialoghandler.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBException;

import gov.dot.its.jpo.sdcsdw.Models.AdvisoryBroadcast;
import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationBundle;
import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationData;
import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationDataDistribution;
import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationDataDistributionList;
import gov.dot.its.jpo.sdcsdw.Models.AsdRecords;
import gov.dot.its.jpo.sdcsdw.Models.DataReceipt;
import gov.dot.its.jpo.sdcsdw.Models.DialogID;
import gov.dot.its.jpo.sdcsdw.Models.ServiceResponse;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.service.MessageCreator;
import gov.dot.its.jpo.sdcsdw.xerjaxbcodec.XerJaxbCodec;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MessageCreatorTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public MessageCreatorTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(MessageCreatorTest.class);
	}

	public void testCreateServiceResponse() {
		DialogID expectedDialog = new DialogID();
		expectedDialog.setAdvSitDatDist("");
		ServiceResponse actualServiceResponse = MessageCreator.createServiceResponse(expectedDialog, "TESTGROUP",
				"TESTREQUEST", "TESTHASH", "483743530", "-1316439680", "241562500", "-723472400");

		assertEquals(expectedDialog, actualServiceResponse.getDialogID());
		assertEquals("TESTGROUP", actualServiceResponse.getGroupID());
		assertEquals("TESTREQUEST", actualServiceResponse.getRequestID());
		assertEquals("TESTHASH", actualServiceResponse.getHash());

	}

	public void testCreateMultipleAdvisorySituationDataDistribution() {
		// CREATE TEST ASD
		List<AdvisorySituationData> asdList = new ArrayList<AdvisorySituationData>();
		for (int i = 0; i < 801; i++) {
			String rawXER = "<AdvisorySituationData><dialogID><advSitDataDep/></dialogID><seqID><data/></seqID><groupID>00000000</groupID><requestID>88D27197</requestID><timeToLive><week/></timeToLive><serviceRegion><nwCorner><lat>449984590</lat><long>-1110408170</long></nwCorner><seCorner><lat>411046740</lat><long>-1041113120</long></seCorner></serviceRegion><asdmDetails><asdmID>88D27197</asdmID><asdmType><tim/></asdmType><distType>10</distType><startTime><year>2017</year><month>12</month><day>1</day><hour>17</hour><minute>47</minute></startTime><stopTime><year>2018</year><month>12</month><day>1</day><hour>17</hour><minute>47</minute></stopTime><advisoryMessage>03805E001F5B70D07930EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0BAD7580160307F82C5BF14005C00854E7C8A5A2A72E2D933D30579AAAA8B555508CE4539F22968A9CB8B64CF4C03F88600003E8F775D9B0</advisoryMessage></asdmDetails></AdvisorySituationData>";
			AdvisorySituationData advSitData = null;
			try {
				advSitData = (AdvisorySituationData) XerJaxbCodec.XerToJaxbPojo(rawXER);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			asdList.add(advSitData);
		}

		DialogID expectedDialog = new DialogID();
		expectedDialog.setAdvSitDatDist("");

		AdvisorySituationDataDistributionList distList = MessageCreator
				.createAdvisorySituationDataDistributionList(asdList, expectedDialog, "TESTGROUP", "TESTREQUEST");

		assertEquals(21, distList.getDistributionList().size());
	}

	public void testCreateMultipleAdvisorySituationDataDistributionWithoutBcastInstructionDefined() {
		// CREATE TEST ASD
		List<AdvisorySituationData> asdList = new ArrayList<AdvisorySituationData>();
		for (int i = 0; i < 801; i++) {
			String rawXER = "<AdvisorySituationData><dialogID><advSitDataDep/></dialogID><seqID><data/></seqID><groupID>00000000</groupID><requestID>88D27197</requestID><timeToLive><week/></timeToLive><serviceRegion><nwCorner><lat>449984590</lat><long>-1110408170</long></nwCorner><seCorner><lat>411046740</lat><long>-1041113120</long></seCorner></serviceRegion><asdmDetails><asdmID>88D27197</asdmID><asdmType><tim/></asdmType><distType>10</distType><advisoryMessage>03805E001F5B70D07930EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0BAD7580160307F82C5BF14005C00854E7C8A5A2A72E2D933D30579AAAA8B555508CE4539F22968A9CB8B64CF4C03F88600003E8F775D9B0</advisoryMessage></asdmDetails></AdvisorySituationData>";
			AdvisorySituationData advSitData = null;
			try {
				advSitData = (AdvisorySituationData) XerJaxbCodec.XerToJaxbPojo(rawXER);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			asdList.add(advSitData);
		}

		DialogID expectedDialog = new DialogID();
		expectedDialog.setAdvSitDatDist("");

		AdvisorySituationDataDistributionList distList = MessageCreator
				.createAdvisorySituationDataDistributionList(asdList, expectedDialog, "TESTGROUP", "TESTREQUEST");

		assertEquals(21, distList.getDistributionList().size());
	}

	public void testCreateMultipleAdvisorySituationDataDistributionWithBCastInstructionsAndOnlyStartTime() {
		// CREATE TEST ASD
		List<AdvisorySituationData> asdList = new ArrayList<AdvisorySituationData>();
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
		GregorianCalendar cal = new GregorianCalendar();
		int year = 2017;
		int total = 365;

		for (int d = 1; d <= total; d++) {
			cal.set(Calendar.DAY_OF_YEAR, d);
			Date date = cal.getTime();

			String rawXER = "<AdvisorySituationData><dialogID><advSitDataDep/></dialogID><seqID><data/></seqID><groupID>00000000</groupID><requestID>88D27197</requestID><timeToLive><week/></timeToLive><serviceRegion><nwCorner><lat>449984590</lat><long>-1110408170</long></nwCorner><seCorner><lat>411046740</lat><long>-1041113120</long></seCorner></serviceRegion><asdmDetails><asdmID>88D27197</asdmID><asdmType><tim/></asdmType><distType>10</distType><startTime><year>2017</year><month>12</month><day>"
					+ date.getDate()
					+ "</day><hour>17</hour><minute>47</minute></startTime><advisoryMessage>03805E001F5B70D07930EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0BAD7580160307F82C5BF14005C00854E7C8A5A2A72E2D933D30579AAAA8B555508CE4539F22968A9CB8B64CF4C03F88600003E8F775D9B0</advisoryMessage></asdmDetails></AdvisorySituationData>";
			AdvisorySituationData advSitData = null;
			try {
				advSitData = (AdvisorySituationData) XerJaxbCodec.XerToJaxbPojo(rawXER);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			asdList.add(advSitData);
		}

		DialogID expectedDialog = new DialogID();
		expectedDialog.setAdvSitDatDist("");

		AdvisorySituationDataDistributionList distList = MessageCreator
				.createAdvisorySituationDataDistributionList(asdList, expectedDialog, "TESTGROUP", "TESTREQUEST");

		
		for (AdvisorySituationDataDistribution distribution : distList.getDistributionList()) {
			for (AdvisorySituationBundle bundle : distribution.getAsdBundles().getAdvisorySituationBundle()) {
				for (AdvisoryBroadcast broadcast : bundle.getAsdRecords().getAdvisoryBroadcast()) {
					assertTrue(Integer.parseInt(broadcast.getBroadcastInst().getBiDeliveryStop().getDay())<=31);
					assertTrue(Integer.parseInt(broadcast.getBroadcastInst().getBiDeliveryStop().getDay())>0);
					assertTrue(Integer.parseInt(broadcast.getBroadcastInst().getBiDeliveryStop().getMonth())>0);
					assertTrue(Integer.parseInt(broadcast.getBroadcastInst().getBiDeliveryStop().getMonth())<=12);
				}
			}

		}

		assertEquals(10, distList.getDistributionList().size());
	}
	
	public void testCreateMultipleAdvisorySituationDataDistributionWithBCastInstructionsAndNoStartOrStopTime() {
		// CREATE TEST ASD
		List<AdvisorySituationData> asdList = new ArrayList<AdvisorySituationData>();
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
		GregorianCalendar cal = new GregorianCalendar();
		int year = 2017;
		int total = 365;

		for (int d = 1; d <= total; d++) {
			cal.set(Calendar.DAY_OF_YEAR, d);
			Date date = cal.getTime();

			String rawXER = "<AdvisorySituationData><dialogID><advSitDataDep/></dialogID><seqID><data/></seqID><groupID>00000000</groupID><requestID>88D27197</requestID><timeToLive><week/></timeToLive><serviceRegion><nwCorner><lat>449984590</lat><long>-1110408170</long></nwCorner><seCorner><lat>411046740</lat><long>-1041113120</long></seCorner></serviceRegion><asdmDetails><asdmID>88D27197</asdmID><asdmType><tim/></asdmType><distType>10</distType><advisoryMessage>03805E001F5B70D07930EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0BAD7580160307F82C5BF14005C00854E7C8A5A2A72E2D933D30579AAAA8B555508CE4539F22968A9CB8B64CF4C03F88600003E8F775D9B0</advisoryMessage></asdmDetails></AdvisorySituationData>";
			AdvisorySituationData advSitData = null;
			try {
				advSitData = (AdvisorySituationData) XerJaxbCodec.XerToJaxbPojo(rawXER);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			asdList.add(advSitData);
		}

		DialogID expectedDialog = new DialogID();
		expectedDialog.setAdvSitDatDist("");

		AdvisorySituationDataDistributionList distList = MessageCreator
				.createAdvisorySituationDataDistributionList(asdList, expectedDialog, "TESTGROUP", "TESTREQUEST");

		
		for (AdvisorySituationDataDistribution distribution : distList.getDistributionList()) {
			for (AdvisorySituationBundle bundle : distribution.getAsdBundles().getAdvisorySituationBundle()) {
				for (AdvisoryBroadcast broadcast : bundle.getAsdRecords().getAdvisoryBroadcast()) {
					assertTrue(Integer.parseInt(broadcast.getBroadcastInst().getBiDeliveryStop().getDay())<=31);
					assertTrue(Integer.parseInt(broadcast.getBroadcastInst().getBiDeliveryStop().getDay())>0);
					assertTrue(Integer.parseInt(broadcast.getBroadcastInst().getBiDeliveryStop().getMonth())>0);
					assertTrue(Integer.parseInt(broadcast.getBroadcastInst().getBiDeliveryStop().getMonth())<=12);
				}
			}

		}

		assertEquals(10, distList.getDistributionList().size());
	}

	public void testCreateSingleAdvisorySituationDataDistribution() {
		// CREATE TEST ASD
		List<AdvisorySituationData> asdList = new ArrayList<AdvisorySituationData>();
		for (int i = 0; i < 40; i++) {
			String rawXER = "<AdvisorySituationData><dialogID><advSitDataDep/></dialogID><seqID><data/></seqID><groupID>00000000</groupID><requestID>88D27197</requestID><timeToLive><week/></timeToLive><serviceRegion><nwCorner><lat>449984590</lat><long>-1110408170</long></nwCorner><seCorner><lat>411046740</lat><long>-1041113120</long></seCorner></serviceRegion><asdmDetails><asdmID>88D27197</asdmID><asdmType><tim/></asdmType><distType>10</distType><startTime><year>2017</year><month>12</month><day>1</day><hour>17</hour><minute>47</minute></startTime><stopTime><year>2018</year><month>12</month><day>1</day><hour>17</hour><minute>47</minute></stopTime><advisoryMessage>03805E001F5B70D07930EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0BAD7580160307F82C5BF14005C00854E7C8A5A2A72E2D933D30579AAAA8B555508CE4539F22968A9CB8B64CF4C03F88600003E8F775D9B0</advisoryMessage></asdmDetails></AdvisorySituationData>";
			AdvisorySituationData advSitData = null;
			try {
				advSitData = (AdvisorySituationData) XerJaxbCodec.XerToJaxbPojo(rawXER);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			asdList.add(advSitData);
		}

		DialogID expectedDialog = new DialogID();
		expectedDialog.setAdvSitDatDist("");

		AdvisorySituationDataDistributionList distList = MessageCreator
				.createAdvisorySituationDataDistributionList(asdList, expectedDialog, "TESTGROUP", "TESTREQUEST");

		assertEquals(1, distList.getDistributionList().size());

	}

	public void testCreateSingleEmptyAdvisorySituationDataDistribution() {
		// CREATE TEST ASD
		List<AdvisorySituationData> asdList = new ArrayList<AdvisorySituationData>();
		for (int i = 0; i < 0; i++) {
			String rawXER = "<AdvisorySituationData><dialogID><advSitDataDep/></dialogID><seqID><data/></seqID><groupID>00000000</groupID><requestID>88D27197</requestID><timeToLive><week/></timeToLive><serviceRegion><nwCorner><lat>449984590</lat><long>-1110408170</long></nwCorner><seCorner><lat>411046740</lat><long>-1041113120</long></seCorner></serviceRegion><asdmDetails><asdmID>88D27197</asdmID><asdmType><tim/></asdmType><distType>10</distType><startTime><year>2017</year><month>12</month><day>1</day><hour>17</hour><minute>47</minute></startTime><stopTime><year>2018</year><month>12</month><day>1</day><hour>17</hour><minute>47</minute></stopTime><advisoryMessage>03805E001F5B70D07930EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0BAD7580160307F82C5BF14005C00854E7C8A5A2A72E2D933D30579AAAA8B555508CE4539F22968A9CB8B64CF4C03F88600003E8F775D9B0</advisoryMessage></asdmDetails></AdvisorySituationData>";
			AdvisorySituationData advSitData = null;
			try {
				advSitData = (AdvisorySituationData) XerJaxbCodec.XerToJaxbPojo(rawXER);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			asdList.add(advSitData);
		}

		DialogID expectedDialog = new DialogID();
		expectedDialog.setAdvSitDatDist("");

		AdvisorySituationDataDistributionList distList = MessageCreator
				.createAdvisorySituationDataDistributionList(asdList, expectedDialog, "TESTGROUP", "TESTREQUEST");

		assertEquals(1, distList.getDistributionList().size());
	}

	public void testCreateDataReceipt() {
		DialogID expectedDialog = new DialogID();
		expectedDialog.setAdvSitDatDist("");
		DataReceipt actualDataReceipt = MessageCreator.createDataReceipt(expectedDialog, "TESTGROUP", "TESTREQUEST");
		assertEquals(expectedDialog, actualDataReceipt.getDialogID());
		assertEquals("TESTGROUP", actualDataReceipt.getGroupID());
		assertEquals("TESTREQUEST", actualDataReceipt.getRequestID());
	}

}