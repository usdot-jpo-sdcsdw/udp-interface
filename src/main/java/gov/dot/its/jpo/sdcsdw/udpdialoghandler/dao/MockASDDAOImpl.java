package gov.dot.its.jpo.sdcsdw.udpdialoghandler.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;

public class MockASDDAOImpl implements ASDDAOInterface {
	// Return mock data -- for testing

	public void setMockMessageCount(int msgCount) {
		this.mockMessagesToMake = msgCount;
	}

	public int getMockMessageCount() {
		return this.mockMessagesToMake;
	}

	public void setInsertBadData(boolean insertBadData) {
		this.insertBadData = insertBadData;
	}

	public List<Document> getAllTIMData() {
		List<Document> result = new ArrayList<Document>();

		for (int i = 0; i < getMockMessageCount(); i++) {
			result.add(Document.parse(
					"{ \"_id\" : { \"$oid\" : \"5a2af6cde9d4b0e460e5725b\" }, \"systemDepositName\" : \"SDW 2.3\", \"encodeType\" : \"HEX\", \"encodedMsg\" : \"c44000000000001869f0001869f4a6e258e4965c4b50080029b88b9b459716ba6200000000ad9a010d00700ca003ec440200000000006e75a260104e5371223f0b2e2a5cc8003fffc9f8800d202d05080fca6e2447e165c54b9900004b0dfffe0033165c50154dc460f83165c52674dc469033165c557fcdc4a0b93165c54b9cdc4a9283165c532dcdc4ae0f084000200800\", \"dialogID\" : 0, \"createdAt\" : { \"$date\" : 1512765133374 }, \"expireAt\" : { \"$date\" : 1512765133374 } }"));
		}

		System.out.println(result.size() + " RECORDS RETRIEVED FROM MONGO (MOCK)");
		if (insertBadData) {
			result.add(Document.parse(
					"{ \"_id\" : { \"$oid\" : \"5a2af6cde9d4b0e460e5725b\" }, \"systemDepositName\" : \"SDW 2.3\", \"encodeType\" : \"HEX\", \"encodedMsg\" : \"4440000000088D271976283B90A7148D2B0A89C49F8A85A7763BFC46938CBA107E1C0C6F7E2C0C6F0C20700BC003EB6E1A0F261D93846D600000000001EEEBB360603D4E7C8A5A2A72E2D933D3AAAA200007E175AEB002C060FF058B7E2800B8010A9914B454E5C5B267A60AF3555516AAAA119C8A73E452D1539716C99E9807F10C00007D1EEEBB3600\", \"dialogID\" : 0, \"createdAt\" : { \"$date\" : 1512765133374 }, \"expireAt\" : { \"$date\" : 1512765133374 } }"));

			result.add(Document.parse(
					"{ \"_id\" : { \"$oid\" : \"5a2af6cde9d4b0e460e5725b\" }, \"systemDepositName\" : \"SDW 2.3\", \"encodeType\" : \"HEX\", \"encodedMsg\" : \"4000000088D271976283B90A9C49F8A85A7763BFC46938CBA107E1C0C6F7E2C0C6F0C20700BC003EB6E1A0F261D93846D600000000001EEEBB360603D4E7C8A5A2A72E2D933D3AAAA200007E175AEB002C060FF058B7E2800B8010A9CF914B454E5C5B267A60AF3555516AAAA119C8A73E452D1539716C99E9807F10C00007D1EEEBB3600\", \"dialogID\" : 0, \"createdAt\" : { \"$date\" : 1512765133374 }, \"expireAt\" : { \"$date\" : 1512765133374 } }"));

			logger.info("INSERTED 2 BAD RECORDS!");
		}

		return result;
	}

	private boolean insertBadData = false;
	private int mockMessagesToMake = 45;
	private final static Logger logger = Logger.getLogger(MockASDDAOImpl.class);
}
