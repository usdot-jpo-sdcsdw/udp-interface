package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.DAO;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Service.MessageCreator;

public class ASDDAO implements ASDDAOInterface {

	public ASDDAO(MongoDatabase mongo, String DbName, String collectionName) {
		// Initialize connection to backend DB
		this.collection = mongo.getCollection(collectionName);

	}

	/**
	 * Return ALL ASD data from Mongo
	 * 
	 * @return
	 */
	public List<Document> getAllTIMData() {

		MongoCursor<Document> cursor = this.collection.find().iterator();
		List<Document> result = new ArrayList<Document>();
		try {
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				result.add(doc);
			}
		} finally {
			cursor.close();
		}
		logger.info(result.size() + " RECORDS RETRIEVED FROM MONGO");
		return result;
	}

	private MongoCollection<Document> collection;
	private final static Logger logger = Logger.getLogger(ASDDAO.class);

}
