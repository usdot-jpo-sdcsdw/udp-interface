package gov.dot.its.jpo.sdcsdw.udpdialoghandler.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.BeforeClass;

import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationDataDistribution;
import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationDataDistributionList;
import gov.dot.its.jpo.sdcsdw.Models.DataAcceptance;
import gov.dot.its.jpo.sdcsdw.Models.DataReceipt;
import gov.dot.its.jpo.sdcsdw.Models.DataRequest;
import gov.dot.its.jpo.sdcsdw.Models.Destination;
import gov.dot.its.jpo.sdcsdw.Models.DialogID;
import gov.dot.its.jpo.sdcsdw.Models.DialogMessage;
import gov.dot.its.jpo.sdcsdw.Models.NwCorner;
import gov.dot.its.jpo.sdcsdw.Models.SeCorner;
import gov.dot.its.jpo.sdcsdw.Models.SemiSequenceID;
import gov.dot.its.jpo.sdcsdw.Models.ServiceRegion;
import gov.dot.its.jpo.sdcsdw.Models.ServiceRequest;
import gov.dot.its.jpo.sdcsdw.Models.ServiceResponse;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.dao.MockASDDAOImpl;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception.ProcessingFailedException;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.service.MessageProcessor;

import org.junit.Test;

public class MessageProcessorTest {

	@BeforeClass
	public static void setUp() {
		mockASDDAO = new MockASDDAOImpl();
		messageProcessor = new MessageProcessor(mockASDDAO);
	}

	@Test
	public void testProcessServiceRequest() {

		ServiceRequest serviceRequest = new ServiceRequest();

		DialogID dialogID = DialogID.fromValue("advSitDatDist");
		serviceRequest.setDialogID(dialogID);

		serviceRequest.setSeqID(SemiSequenceID.SVC_REQ);

		serviceRequest.setGroupID("0000000");
		serviceRequest.setRequestID("E054E21B");

		Destination dest = new Destination();
		dest.setPort("46750");
		serviceRequest.setDestination(dest);

		DialogMessage responseObject = null;
		try {
			responseObject = messageProcessor.getResponseMessage(serviceRequest, Hex.decodeHex("8c000000001c0a9c436b69e0"));
		} catch (NoSuchElementException | DecoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProcessingFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(responseObject instanceof ServiceResponse);
		ServiceResponse actualServiceResponse = (ServiceResponse) responseObject;
		assertEquals(serviceRequest.getGroupID(), actualServiceResponse.getGroupID());
		assertEquals(serviceRequest.getRequestID(), actualServiceResponse.getRequestID());
		// That long string is the SHA256 hash value of the Service Request hex as a
		// byte array
		assertEquals("6B5FF555CD8EA68A7FFA157D096D0CF9E7D1F573F506781A92721D474E2A2CD2",
				actualServiceResponse.getHash());
		assertEquals(SemiSequenceID.SVC_RESP, actualServiceResponse.getSeqID());
	}

	@Test
	public void testProcessDataRequestExpectASingleDistribution() {

		DataRequest dataRequest = new DataRequest();

		DialogID dialogID = DialogID.fromValue("advSitDatDist");
		dataRequest.setDialogID(dialogID);
		dataRequest.setDistType("02");
		dataRequest.setGroupID("0000000");
		dataRequest.setRequestID("E054E21B");

		dataRequest.setSeqID(SemiSequenceID.DATA_REQ);

		// ServiceRegion
		NwCorner nwCorner = new NwCorner();
		nwCorner.setLat("483743530");
		nwCorner.setLong("-1316439680");

		SeCorner seCorner = new SeCorner();
		seCorner.setLat("241562500");
		seCorner.setLong("-723472400");

		ServiceRegion serviceRegion = new ServiceRegion();
		serviceRegion.setNwCorner(nwCorner);
		serviceRegion.setSeCorner(seCorner);
		dataRequest.setServiceRegion(serviceRegion);

		mockASDDAO.setMockMessageCount(20);
		DialogMessage responseObject = null;
		try {
			responseObject = messageProcessor.getResponseMessage(dataRequest,
					Hex.decodeHex("0c400000001c0a9c436293d20150e6945bf88815b908805503de04"));
		} catch (NoSuchElementException | ProcessingFailedException | DecoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertTrue(responseObject instanceof AdvisorySituationDataDistributionList);
		AdvisorySituationDataDistributionList actualAdvisorySituationDataDistributionList = (AdvisorySituationDataDistributionList) responseObject;
		assertEquals(1, actualAdvisorySituationDataDistributionList.getDistributionList().size());
		assertTrue(actualAdvisorySituationDataDistributionList.getDistributionList()
				.get(0) instanceof AdvisorySituationDataDistribution);

		assertEquals("20", actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getRecordCount());
		assertEquals("2", actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getBundleCount());

		assertEquals(dataRequest.getDialogID().getString(),
				actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getDialogID().getString());

		assertEquals(dataRequest.getRequestID(), actualAdvisorySituationDataDistributionList.getRequestID());
		assertEquals(dataRequest.getRequestID(),
				actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getRequestID());

		assertEquals(dataRequest.getGroupID(),
				actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getGroupID());

	}

	@Test
	public void testProcessDataRequestExpectAnEmptyDistribution() {

		DataRequest dataRequest = new DataRequest();

		DialogID dialogID = DialogID.fromValue("advSitDatDist");
		dataRequest.setDialogID(dialogID);
		dataRequest.setDistType("02");
		dataRequest.setGroupID("0000000");
		dataRequest.setRequestID("E054E21B");

		dataRequest.setSeqID(SemiSequenceID.DATA_REQ);

		// ServiceRegion
		NwCorner nwCorner = new NwCorner();
		nwCorner.setLat("483743530");
		nwCorner.setLong("-1316439680");

		SeCorner seCorner = new SeCorner();
		seCorner.setLat("241562500");
		seCorner.setLong("-723472400");

		ServiceRegion serviceRegion = new ServiceRegion();
		serviceRegion.setNwCorner(nwCorner);
		serviceRegion.setSeCorner(seCorner);
		dataRequest.setServiceRegion(serviceRegion);

		mockASDDAO.setMockMessageCount(0);
		DialogMessage responseObject = null;
		try {
			responseObject = messageProcessor.getResponseMessage(dataRequest,
					Hex.decodeHex("0c400000001c0a9c436293d20150e6945bf88815b908805503de04"));
		} catch (NoSuchElementException | ProcessingFailedException | DecoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertTrue(responseObject instanceof AdvisorySituationDataDistributionList);
		AdvisorySituationDataDistributionList actualAdvisorySituationDataDistributionList = (AdvisorySituationDataDistributionList) responseObject;
		assertEquals(1, actualAdvisorySituationDataDistributionList.getDistributionList().size());
		assertTrue(actualAdvisorySituationDataDistributionList.getDistributionList()
				.get(0) instanceof AdvisorySituationDataDistribution);

		assertEquals("0", actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getRecordCount());
		assertEquals("0", actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getBundleCount());

		assertEquals(dataRequest.getDialogID().getString(),
				actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getDialogID().getString());

		assertEquals(dataRequest.getRequestID(), actualAdvisorySituationDataDistributionList.getRequestID());
		assertEquals(dataRequest.getRequestID(),
				actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getRequestID());

		assertEquals(dataRequest.getGroupID(),
				actualAdvisorySituationDataDistributionList.getDistributionList().get(0).getGroupID());

	}

	@Test
	public void testProcessDataRequestExpectMultpleDistributions() {

		DataRequest dataRequest = new DataRequest();

		DialogID dialogID = DialogID.fromValue("advSitDatDist");
		dataRequest.setDialogID(dialogID);
		dataRequest.setDistType("02");
		dataRequest.setGroupID("0000000");
		dataRequest.setRequestID("E054E21B");

		dataRequest.setSeqID(SemiSequenceID.DATA_REQ);

		// ServiceRegion
		NwCorner nwCorner = new NwCorner();
		nwCorner.setLat("483743530");
		nwCorner.setLong("-1316439680");

		SeCorner seCorner = new SeCorner();
		seCorner.setLat("241562500");
		seCorner.setLong("-723472400");

		ServiceRegion serviceRegion = new ServiceRegion();
		serviceRegion.setNwCorner(nwCorner);
		serviceRegion.setSeCorner(seCorner);
		dataRequest.setServiceRegion(serviceRegion);

		mockASDDAO.setMockMessageCount(80);
		DialogMessage responseObject = null;
		try {
			responseObject = messageProcessor.getResponseMessage(dataRequest,
					Hex.decodeHex("0c400000001c0a9c436293d20150e6945bf88815b908805503de04"));
		} catch (NoSuchElementException | ProcessingFailedException | DecoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertTrue(responseObject instanceof AdvisorySituationDataDistributionList);
		AdvisorySituationDataDistributionList actualAdvisorySituationDataDistributionList = (AdvisorySituationDataDistributionList) responseObject;
		assertEquals(2, actualAdvisorySituationDataDistributionList.getDistributionList().size());

		assertEquals(dataRequest.getRequestID(), actualAdvisorySituationDataDistributionList.getRequestID());
		for (AdvisorySituationDataDistribution distribution : actualAdvisorySituationDataDistributionList
				.getDistributionList()) {
			assertTrue(distribution instanceof AdvisorySituationDataDistribution);

			assertEquals("80", distribution.getRecordCount());
			assertEquals("8", distribution.getBundleCount());

			assertEquals(dataRequest.getDialogID().getString(), distribution.getDialogID().getString());

			assertEquals(dataRequest.getRequestID(), distribution.getRequestID());

			assertEquals(dataRequest.getGroupID(), distribution.getGroupID());
		}

	}
	
	
	
	@Test
	public void testProcessDataRequestExpectMultpleDistributionsNonBoundary() {

		DataRequest dataRequest = new DataRequest();

		DialogID dialogID = DialogID.fromValue("advSitDatDist");
		dataRequest.setDialogID(dialogID);
		dataRequest.setDistType("02");
		dataRequest.setGroupID("0000000");
		dataRequest.setRequestID("E054E21B");

		dataRequest.setSeqID(SemiSequenceID.DATA_REQ);

		// ServiceRegion
		NwCorner nwCorner = new NwCorner();
		nwCorner.setLat("483743530");
		nwCorner.setLong("-1316439680");

		SeCorner seCorner = new SeCorner();
		seCorner.setLat("241562500");
		seCorner.setLong("-723472400");

		ServiceRegion serviceRegion = new ServiceRegion();
		serviceRegion.setNwCorner(nwCorner);
		serviceRegion.setSeCorner(seCorner);
		dataRequest.setServiceRegion(serviceRegion);

		mockASDDAO.setMockMessageCount(357);
		DialogMessage responseObject = null;
		try {
			responseObject = messageProcessor.getResponseMessage(dataRequest,
					Hex.decodeHex("0c400000001c0a9c436293d20150e6945bf88815b908805503de04"));
		} catch (NoSuchElementException | ProcessingFailedException | DecoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertTrue(responseObject instanceof AdvisorySituationDataDistributionList);
		AdvisorySituationDataDistributionList actualAdvisorySituationDataDistributionList = (AdvisorySituationDataDistributionList) responseObject;
		
		assertEquals(9, actualAdvisorySituationDataDistributionList.getDistributionList().size());
		
		
		for (AdvisorySituationDataDistribution distribution : actualAdvisorySituationDataDistributionList
				.getDistributionList()) {
			System.out.println(distribution.getRecordCount());
		}
		
		assertEquals(dataRequest.getRequestID(), actualAdvisorySituationDataDistributionList.getRequestID());
		for (AdvisorySituationDataDistribution distribution : actualAdvisorySituationDataDistributionList
				.getDistributionList()) {
			assertTrue(distribution instanceof AdvisorySituationDataDistribution);
			
			assertEquals("357", distribution.getRecordCount());
			assertEquals("36", distribution.getBundleCount());

			assertEquals(dataRequest.getDialogID().getString(), distribution.getDialogID().getString());

			assertEquals(dataRequest.getRequestID(), distribution.getRequestID());

			assertEquals(dataRequest.getGroupID(), distribution.getGroupID());

		}

	}
	

	@Test
	public void testProcessDataRequestExpectMultpleDistributionsWithBadDataInStore() {
		mockASDDAO.setInsertBadData(true);

		DataRequest dataRequest = new DataRequest();

		DialogID dialogID = DialogID.fromValue("advSitDatDist");
		dataRequest.setDialogID(dialogID);
		dataRequest.setDistType("02");
		dataRequest.setGroupID("0000000");
		dataRequest.setRequestID("E054E21B");

		dataRequest.setSeqID(SemiSequenceID.DATA_REQ);

		// ServiceRegion
		NwCorner nwCorner = new NwCorner();
		nwCorner.setLat("483743530");
		nwCorner.setLong("-1316439680");

		SeCorner seCorner = new SeCorner();
		seCorner.setLat("241562500");
		seCorner.setLong("-723472400");

		ServiceRegion serviceRegion = new ServiceRegion();
		serviceRegion.setNwCorner(nwCorner);
		serviceRegion.setSeCorner(seCorner);
		dataRequest.setServiceRegion(serviceRegion);

		mockASDDAO.setMockMessageCount(80);
		DialogMessage responseObject = null;
		try {
			responseObject = messageProcessor.getResponseMessage(dataRequest,
					Hex.decodeHex("0c400000001c0a9c436293d20150e6945bf88815b908805503de04"));
		} catch (NoSuchElementException | ProcessingFailedException | DecoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertTrue(responseObject instanceof AdvisorySituationDataDistributionList);
		AdvisorySituationDataDistributionList actualAdvisorySituationDataDistributionList = (AdvisorySituationDataDistributionList) responseObject;
		assertEquals(2, actualAdvisorySituationDataDistributionList.getDistributionList().size());

		assertEquals(dataRequest.getRequestID(), actualAdvisorySituationDataDistributionList.getRequestID());
		for (AdvisorySituationDataDistribution distribution : actualAdvisorySituationDataDistributionList
				.getDistributionList()) {
			assertTrue(distribution instanceof AdvisorySituationDataDistribution);

			assertEquals("80", distribution.getRecordCount());
			assertEquals("8", distribution.getBundleCount());

			assertEquals(dataRequest.getDialogID().getString(), distribution.getDialogID().getString());

			assertEquals(dataRequest.getRequestID(), distribution.getRequestID());

			assertEquals(dataRequest.getGroupID(), distribution.getGroupID());
		}
		
		mockASDDAO.setInsertBadData(false);
	}

	@Test
	public void testProcessDataAcceptance() {
		DataAcceptance dataAcceptance = new DataAcceptance();

		DialogID dialogID = DialogID.fromValue("advSitDatDist");
		dataAcceptance.setDialogID(dialogID);

		dataAcceptance.setGroupID("0000000");
		dataAcceptance.setRequestID("E054E21B");

		dataAcceptance.setSeqID(SemiSequenceID.ACCEPT);

		DataReceipt actualDataReceipt = null;

		try {
			actualDataReceipt = (DataReceipt) messageProcessor.getResponseMessage(dataAcceptance,
					Hex.decodeHex("194000000038153886c0"));
		} catch (NoSuchElementException | DecoderException | ProcessingFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(dataAcceptance.getGroupID(), actualDataReceipt.getGroupID());
		assertEquals(dataAcceptance.getRequestID(), actualDataReceipt.getRequestID());
		assertEquals(dataAcceptance.getDialogID(), actualDataReceipt.getDialogID());
		assertEquals(SemiSequenceID.RECEIPT, actualDataReceipt.getSeqID());

	}

	@Test
	public void testProcessUnexpectedMessage() {
		DataReceipt dataReceipt = new DataReceipt();

		DialogID dialogID = DialogID.fromValue("advSitDatDist");
		dataReceipt.setDialogID(dialogID);

		dataReceipt.setGroupID("0000000");
		dataReceipt.setRequestID("E054E21B");

		dataReceipt.setSeqID(SemiSequenceID.RECEIPT);

		DialogMessage unexpectedObject = null;
		try {
			unexpectedObject = (DataReceipt) messageProcessor.getResponseMessage(dataReceipt,
					Hex.decodeHex("198000000038153886c0"));
			fail("Exepected to fail while processing DataReceipt. The DataReceipt is not one of the expected"
					+ " incoming message types.");
		} catch (NoSuchElementException | DecoderException | ProcessingFailedException e) {
			assertEquals(
					"The message was not one of the expected message types: (ServiceRequest,DataRequest,DataAcceptance)",
					e.getMessage());
		}

	}

	/*
	 * 
	 * //add one bad message result.add(Document.parse(
	 * "{ \"_id\" : { \"$oid\" : \"5a2af6cde9d4b0e460e5725b\" }, \"systemDepositName\" : \"SDW 2.3\", \"encodeType\" : \"HEX\", \"encodedMsg\" : \"40000000088D271976283B90A7148D2B0A89C49F8A85A7763BFC46938CBA107E1C0C6F7E2C0C6F0C20700BC003EB6E1A0F261D93846D600000000001EEEBB360603D4E7C8A5A2A72E2D933D3AAAA200007E175AEB002C060FF058B7E2800B8010A9CF914B454E5C5B267A60AF3555516AAAA119C8A73E452D1539716C99E9807F10C00007D1EEEBB3600\", \"dialogID\" : 0, \"createdAt\" : { \"$date\" : 1512765133374 }, \"expireAt\" : { \"$date\" : 1512765133374 } }"
	 * ));
	 * 
	 */

	private static MessageProcessor messageProcessor;
	private static MockASDDAOImpl mockASDDAO;

}