package gov.dot.its.jpo.sdcsdw.udpdialoghandler.service;

import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationDataDistribution;
import gov.dot.its.jpo.sdcsdw.Models.DataReceipt;
import gov.dot.its.jpo.sdcsdw.Models.DialogMessage;
import gov.dot.its.jpo.sdcsdw.Models.ServiceResponse;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.CodecFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.FormattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.UnformattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.RawPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.RawXerData;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.dao.MockASDDAOImpl;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception.KeyNotFoundException;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception.ProcessingFailedException;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.model.Response;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.service.DialogHandler;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.service.MessageProcessor;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.session.LocalSessionHandler;
import gov.dot.its.jpo.sdcsdw.xerjaxbcodec.XerJaxbCodec;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.JAXBException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Unit test for simple DialogHandler.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DialogHandlerTest

{

	@BeforeClass
	public static void setUp() {
		sessionHandler = new LocalSessionHandler();
		mockASDDAO = new MockASDDAOImpl();
		messageProcessor = new MessageProcessor(mockASDDAO);
		dialogHandler = new DialogHandler(sessionHandler, messageProcessor);

		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void AprocessAndCreateResponseForServiceRequest() {
		byte[] buffer = null;

		for (String serviceRequestMessage : serviceRequestMessages) {

			try {
				buffer = Hex.decodeHex(serviceRequestMessage.replace(" ", ""));
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Response response = null;
			try {
				response = dialogHandler.processAndCreateResponse(address, buffer);
			} catch (ProcessingFailedException e) {
				fail("Expected a proper response object with a ServiceResponse. Failed while processing: "
						+ e.getMessage());
			}

			assertEquals(46850, response.getDestination().getPort());
			assertEquals(address.getHostAddress(), response.getDestination().getAddress().getHostAddress());

			String serviceResponseXER = null;
			try {
				serviceResponseXER = PerXerCodec.perToXer(Asn1Types.getAsn1TypeByName("ServiceResponse"),
						response.getResponseMessage(), RawPerData.unformatter, RawXerData.formatter);
			} catch (CodecFailedException | FormattingFailedException | UnformattingFailedException e) {
				fail(String.format(
						"Failed converting service response bytes to XER using the ASN1 encoder/decoder. Message: %s, Error: %s\n",
						Hex.encodeHexString(response.getResponseMessage()), e.getMessage()));
			}

			DialogMessage responseObject = null;
			try {
				responseObject = XerJaxbCodec.XerToJaxbPojo(serviceResponseXER);
			} catch (JAXBException e) {
				fail(String.format(
						"Failed converting service response XER to POJO XerJaxbCodec. Message: %s, Error: %s\n",
						serviceResponseXER, e.getMessage()));
			}

			assertTrue(responseObject instanceof ServiceResponse);
			ServiceResponse serviceResponse = (ServiceResponse) responseObject;
			assertEquals(Hex.encodeHexString(digest.digest(buffer)).toUpperCase(), serviceResponse.getHash());
		}

	}

	@Test
	public void BprocessAndCreateResponseForDataRequestWithSingleDistribution() {
		byte[] buffer = null;
		mockASDDAO.setMockMessageCount(40);

		for (String dataRequestMessage : dataRequestMessages) {

			try {
				buffer = Hex.decodeHex(dataRequestMessage.replace(" ", ""));
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Response response = null;
			try {
				response = dialogHandler.processAndCreateResponse(address, buffer);
			} catch (ProcessingFailedException e) {
				fail("Expected a proper response object with a AdvisorySituationDataDistributionList. Failed while processing: "
						+ e.getMessage());
			}

			assertEquals(46850, response.getDestination().getPort());
			assertEquals(address.getHostAddress(), response.getDestination().getAddress().getHostAddress());
			assertEquals(1, response.getResponseList().size());
			for (byte[] distribution : response.getResponseList()) {
				String distributionXER = null;
				try {
					distributionXER = PerXerCodec.perToXer(
							Asn1Types.getAsn1TypeByName("AdvisorySituationDataDistribution"), distribution,
							RawPerData.unformatter, RawXerData.formatter);
				} catch (CodecFailedException | FormattingFailedException | UnformattingFailedException e) {
					fail(String.format(
							"Failed converting distribution bytes to XER using the ASN1 encoder/decoder. Message: %s, Error: %s\n",
							Hex.encodeHexString(distribution), e.getMessage()));
				}

				DialogMessage responseObject = null;
				try {
					responseObject = XerJaxbCodec.XerToJaxbPojo(distributionXER);
				} catch (JAXBException e) {
					fail(String.format(
							"Failed converting distribution XER to POJO XerJaxbCodec. Message: %s, Error: %s\n",
							distributionXER, e.getMessage()));
				}

				assertTrue(responseObject instanceof AdvisorySituationDataDistribution);
				AdvisorySituationDataDistribution distributionResponse = (AdvisorySituationDataDistribution) responseObject;

			}
		}

	}
	
	@Test
	public void B2processAndCreateResponseForDataRequestWithSingleDistribution() {
		byte[] buffer = null;
		mockASDDAO.setMockMessageCount(1);

		for (String dataRequestMessage : dataRequestMessages) {

			try {
				buffer = Hex.decodeHex(dataRequestMessage.replace(" ", ""));
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Response response = null;
			try {
				response = dialogHandler.processAndCreateResponse(address, buffer);
			} catch (ProcessingFailedException e) {
				fail("Expected a proper response object with a AdvisorySituationDataDistributionList. Failed while processing: "
						+ e.getMessage());
			}

			assertEquals(46850, response.getDestination().getPort());
			assertEquals(address.getHostAddress(), response.getDestination().getAddress().getHostAddress());
			assertEquals(1, response.getResponseList().size());
			for (byte[] distribution : response.getResponseList()) {
				String distributionXER = null;
				try {
					distributionXER = PerXerCodec.perToXer(
							Asn1Types.getAsn1TypeByName("AdvisorySituationDataDistribution"), distribution,
							RawPerData.unformatter, RawXerData.formatter);
				} catch (CodecFailedException | FormattingFailedException | UnformattingFailedException e) {
					fail(String.format(
							"Failed converting distribution bytes to XER using the ASN1 encoder/decoder. Message: %s, Error: %s\n",
							Hex.encodeHexString(distribution), e.getMessage()));
				}

				DialogMessage responseObject = null;
				try {
					responseObject = XerJaxbCodec.XerToJaxbPojo(distributionXER);
				} catch (JAXBException e) {
					fail(String.format(
							"Failed converting distribution XER to POJO XerJaxbCodec. Message: %s, Error: %s\n",
							distributionXER, e.getMessage()));
				}

				assertTrue(responseObject instanceof AdvisorySituationDataDistribution);
				AdvisorySituationDataDistribution distributionResponse = (AdvisorySituationDataDistribution) responseObject;

			}
		}

	}

	@Test
	public void CprocessAndCreateResponseForDataRequestWithSingleEmptyDistribution() {
		byte[] buffer = null;
		mockASDDAO.setMockMessageCount(0);

		for (String dataRequestMessage : dataRequestMessages) {

			try {
				buffer = Hex.decodeHex(dataRequestMessage.replace(" ", ""));
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Response response = null;
			try {
				response = dialogHandler.processAndCreateResponse(address, buffer);
			} catch (ProcessingFailedException e) {
				fail("Expected a proper response object with a AdvisorySituationDataDistributionList. Failed while processing: "
						+ e.getMessage());
			}

			assertEquals(46850, response.getDestination().getPort());
			assertEquals(address.getHostAddress(), response.getDestination().getAddress().getHostAddress());
			assertEquals(1, response.getResponseList().size());
			for (byte[] distribution : response.getResponseList()) {
				String distributionXER = null;
				try {
					distributionXER = PerXerCodec.perToXer(
							Asn1Types.getAsn1TypeByName("AdvisorySituationDataDistribution"), distribution,
							RawPerData.unformatter, RawXerData.formatter);
				} catch (CodecFailedException | FormattingFailedException | UnformattingFailedException e) {
					fail(String.format(
							"Failed converting distribution bytes to XER using the ASN1 encoder/decoder. Message: %s, Error: %s\n",
							Hex.encodeHexString(distribution), e.getMessage()));
				}

				DialogMessage responseObject = null;
				try {
					responseObject = XerJaxbCodec.XerToJaxbPojo(distributionXER);
				} catch (JAXBException e) {
					fail(String.format(
							"Failed converting distribution XER to POJO XerJaxbCodec. Message: %s, Error: %s\n",
							distributionXER, e.getMessage()));
				}

				assertTrue(responseObject instanceof AdvisorySituationDataDistribution);
				AdvisorySituationDataDistribution distributionResponse = (AdvisorySituationDataDistribution) responseObject;

			}
		}

	}

	@Test
	public void DprocessAndCreateResponseForDataRequestWithMultipleDistribution() {
		byte[] buffer = null;
		mockASDDAO.setMockMessageCount(85);

		for (String dataRequestMessage : dataRequestMessages) {

			try {
				buffer = Hex.decodeHex(dataRequestMessage.replace(" ", ""));
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Response response = null;
			try {
				response = dialogHandler.processAndCreateResponse(address, buffer);
			} catch (ProcessingFailedException e) {
				fail("Expected a proper response object with a AdvisorySituationDataDistributionList. Failed while processing: "
						+ e.getMessage());
			}

			assertEquals(46850, response.getDestination().getPort());
			assertEquals(address.getHostAddress(), response.getDestination().getAddress().getHostAddress());
			assertEquals(3, response.getResponseList().size());
			for (byte[] distribution : response.getResponseList()) {
				String distributionXER = null;
				try {
					distributionXER = PerXerCodec.perToXer(
							Asn1Types.getAsn1TypeByName("AdvisorySituationDataDistribution"), distribution,
							RawPerData.unformatter, RawXerData.formatter);
					
				} catch (CodecFailedException | FormattingFailedException | UnformattingFailedException e) {
					fail(String.format(
							"Failed converting distribution bytes to XER using the ASN1 encoder/decoder. Message: %s, Error: %s\n",
							Hex.encodeHexString(distribution), e.getMessage()));
				}

				DialogMessage responseObject = null;
				try {
					responseObject = XerJaxbCodec.XerToJaxbPojo(distributionXER);
				} catch (JAXBException e) {
					fail(String.format(
							"Failed converting distribution XER to POJO XerJaxbCodec. Message: %s, Error: %s\n",
							distributionXER, e.getMessage()));
				}

				assertTrue(responseObject instanceof AdvisorySituationDataDistribution);
				AdvisorySituationDataDistribution distributionResponse = (AdvisorySituationDataDistribution) responseObject;

			}
		}

	}

	@Test
	public void EprocessAndCreateResponseForDataAcceptance() {
		byte[] buffer = null;

		for (String dataAcceptanceMessage : dataAcceptanceMessages) {

			try {
				buffer = Hex.decodeHex(dataAcceptanceMessage.replace(" ", ""));
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Response response = null;
			try {
				response = dialogHandler.processAndCreateResponse(address, buffer);
			} catch (ProcessingFailedException e) {
				fail("Expected a proper response object with a DataReceipt. Failed while processing: "
						+ e.getMessage());
			}

			assertEquals(46850, response.getDestination().getPort());
			assertEquals(address.getHostAddress(), response.getDestination().getAddress().getHostAddress());

			String dataReceiptXER = null;
			try {
				dataReceiptXER = PerXerCodec.perToXer(Asn1Types.getAsn1TypeByName("DataReceipt"),
						response.getResponseMessage(), RawPerData.unformatter, RawXerData.formatter);
			} catch (CodecFailedException | FormattingFailedException | UnformattingFailedException e) {
				fail(String.format(
						"Failed converting data receipt bytes to XER using the ASN1 encoder/decoder. Message: %s, Error: %s\n",
						Hex.encodeHexString(response.getResponseMessage()), e.getMessage()));
			}

			DialogMessage responseObject = null;
			try {
				responseObject = XerJaxbCodec.XerToJaxbPojo(dataReceiptXER);
			} catch (JAXBException e) {
				fail(String.format("Failed converting data receipt XER to POJO XerJaxbCodec. Message: %s, Error: %s\n",
						dataReceiptXER, e.getMessage()));
			}

			assertTrue(responseObject instanceof DataReceipt);
			DataReceipt dataReceipt = (DataReceipt) responseObject;

			// Session Should have been erased:
			try {
				this.sessionHandler.getSession(dataReceipt.getRequestID());
				fail("Session should have been deleted for request id: " + dataReceipt.getRequestID());
			} catch (KeyNotFoundException e) {
				assertEquals("No session exists for request ID: " + dataReceipt.getRequestID(), e.getMessage());
			}

			// Sending a message with no session test:
			try {
				response = dialogHandler.processAndCreateResponse(address, buffer);
				fail("Session was deleted already for this dialog shouldn't have successfully created a response");
			} catch (ProcessingFailedException e) {
				assertEquals("No session exists for the request", e.getMessage());
			}
		}

	}

	@Test
	public void createAndProcessResponseForABadMessage() {
		byte[] buffer = null;

		buffer = "Hello".getBytes();
		try {
			Response responseObject = dialogHandler.processAndCreateResponse(address, buffer);
			fail("Response Object should not have been created, expect a decoding failure");
		} catch (ProcessingFailedException e) {
			assertEquals("Failed to create response object:Message failed to decode, not an ASN1 encoded message",
					e.getMessage());
		}
	}

	@Test
	public void createAndProcessResponseForAnUnxpectedMessage() {
		byte[] buffer = null;

		try {
			// This is a data receipt. We are supposed to SEND data receipt, not receive
			// them
			buffer = Hex.decodeHex("198000000038153886c0");
		} catch (DecoderException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			Response responseObject = dialogHandler.processAndCreateResponse(address, buffer);
			fail("Response Object should not have been created, expect a decoding failure");
		} catch (ProcessingFailedException e) {
			assertEquals("Failed to create response object:The message was not one of the expected message types: "
					+ "(ServiceRequest,DataRequest,DataAcceptance)", e.getMessage());
		}
	}

		
	private String[] serviceRequestMessages = { "8c 00 00 00 00 14 e5 21 ed ab 70 20 ",
			"8c 00 00 00 00 1c 9a 21 49 eb 70 20 ", "8c 00 00 00 00 18 b3 90 2b ab 70 20 ",
			"8c 00 00 00 00 07 4d 27 2e 8b 70 20 ", "8c 00 00 00 00 08 97 dd a2 eb 70 20 ",
			"8c 00 00 00 00 0d 0b 4a 44 8b 70 20 ", "8c 00 00 00 00 02 82 73 88 4b 70 20 ",
			"8c 00 00 00 00 17 94 ae 67 4b 70 20 ", "8c 00 00 00 00 16 26 b8 86 2b 70 20 ",
			"8c 00 00 00 00 1e 25 29 6c 4b 70 20 ", "8c 00 00 00 00 00 91 f6 ac 6b 70 20 ",
			"8c 00 00 00 00 1f 07 88 00 cb 70 20 ", "8c 00 00 00 00 0b 36 d3 a7 6b 70 20 ",
			"8c 00 00 00 00 1c 7c 5c 45 8b 70 20 ", "8c 00 00 00 00 0c ab be 87 8b 70 20 ",
			"8c 00 00 00 00 06 ad fa cc ab 70 20 ", "8c 00 00 00 00 03 82 72 0c eb 70 20 ",
			"8c 00 00 00 00 1d ab 66 a8 ab 70 20 ", "8c 00 00 00 00 1c 28 4e aa cb 70 20 ",
			"8c 00 00 00 00 0b 17 d9 ef cb 70 20 ", "8c 00 00 00 00 03 db 2d 20 4b 70 20 ",
			"8c 00 00 00 00 1b 68 e2 05 2b 70 20 ", "8c 00 00 00 00 09 64 0d ee eb 70 20 ",
			"8c 00 00 00 00 1d 7e 0f 0d 2b 70 20 ", "8c 00 00 00 00 09 d7 20 45 ab 70 20 ",
			"8c 00 00 00 00 1b 16 ee 09 8b 70 20 ", "8c 00 00 00 00 10 f5 37 61 6b 70 20 ",
			"8c 00 00 00 00 02 2a 91 41 8b 70 20 " };

	private String[] dataRequestMessages = {
			"0c 40 00 00 00 14 e5 21 ed a2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1c 9a 21 49 e2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 18 b3 90 2b a2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 07 4d 27 2e 82 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 08 97 dd a2 e2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 0d 0b 4a 44 82 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 02 82 73 88 42 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 17 94 ae 67 42 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 16 26 b8 86 22 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1e 25 29 6c 42 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 00 91 f6 ac 62 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1f 07 88 00 c2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 0b 36 d3 a7 62 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1c 7c 5c 45 82 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 0c ab be 87 82 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 06 ad fa cc a2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 03 82 72 0c e2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1d ab 66 a8 a2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1c 28 4e aa c2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 0b 17 d9 ef c2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 03 db 2d 20 42 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1b 68 e2 05 22 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 09 64 0d ee e2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1d 7e 0f 0d 22 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 09 d7 20 45 a2 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 1b 16 ee 09 82 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 10 f5 37 61 62 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 ",
			"0c 40 00 00 00 02 2a 91 41 82 cb 41 78 00 47 86 8b f8 7d 2b 75 00 a0 ee ba fe 04 " };

	private String[] dataAcceptanceMessages = { "19 40 00 00 00 29 ca 43 db 40 ", "19 40 00 00 00 39 34 42 93 c0 ",
			"19 40 00 00 00 31 67 20 57 40 ", "19 40 00 00 00 0e 9a 4e 5d 00 ", "19 40 00 00 00 11 2f bb 45 c0 ",
			"19 40 00 00 00 1a 16 94 89 00 ", "19 40 00 00 00 05 04 e7 10 80 ", "19 40 00 00 00 2f 29 5c ce 80 ",
			"19 40 00 00 00 2c 4d 71 0c 40 ", "19 40 00 00 00 3c 4a 52 d8 80 ", "19 40 00 00 00 01 23 ed 58 c0 ",
			"19 40 00 00 00 3e 0f 10 01 80 ", "19 40 00 00 00 16 6d a7 4e c0 ", "19 40 00 00 00 38 f8 b8 8b 00 ",
			"19 40 00 00 00 19 57 7d 0f 00 ", "19 40 00 00 00 0d 5b f5 99 40 ", "19 40 00 00 00 07 04 e4 19 c0 ",
			"19 40 00 00 00 3b 56 cd 51 40 ", "19 40 00 00 00 38 50 9d 55 80 ", "19 40 00 00 00 16 2f b3 df 80 ",
			"19 40 00 00 00 07 b6 5a 40 80 ", "19 40 00 00 00 36 d1 c4 0a 40 ", "19 40 00 00 00 12 c8 1b dd c0 ",
			"19 40 00 00 00 3a fc 1e 1a 40 ", "19 40 00 00 00 13 ae 40 8b 40 ", "19 40 00 00 00 36 2d dc 13 00 ",
			"19 40 00 00 00 21 ea 6e c2 c0 ", "19 40 00 00 00 04 55 22 83 00 " };

	private static DialogHandler dialogHandler;
	private static LocalSessionHandler sessionHandler;
	private static MockASDDAOImpl mockASDDAO;
	private static MessageProcessor messageProcessor;
	private static InetAddress address;
	private static MessageDigest digest;

}
