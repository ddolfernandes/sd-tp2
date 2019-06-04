package microgram.impl.mongo;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;

import java.net.UnknownHostException;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import microgram.api.Profile;
import microgram.api.java.Profiles;
import microgram.api.java.Result;

public class MongoProfiles implements Profiles {
	
	

	private MongoDatabase db;
	MongoCollection<Profile> dbCol;
	private static final String MONGO_HOSTNAME = "mongodb://mongo1,mongo2,mongo3";
	private static final String DB_NAME = "mongoDataBase";
	private static final String DB_TABLE = "Profiles";
	private static final String USERID = "userId";
	
	public MongoProfiles() throws UnknownHostException {
		MongoClient mongo = new MongoClient( MONGO_HOSTNAME );
		
		//e suposto ter CodecRegistries antes do fromRegistries e do fromProviders?
		CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(), CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
		
		//nome arbitrario para a database?
		db = mongo.getDatabase(DB_NAME).withCodecRegistry(pojoCodecRegistry);
		dbCol = db.getCollection(DB_TABLE, Profile.class);
		
		//criar index no construtor?
		dbCol.createIndex(Indexes.ascending(USERID), new IndexOptions().unique(true));
		
	}

	@Override
	public Result<Profile> getProfile(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Void> createProfile(Profile profile) {
		
		try{
			dbCol.insertOne(profile);
			return ok();
		}catch( MongoWriteException x ) {
		    return error( CONFLICT );
		}
	}

	@Override
	public Result<Void> deleteProfile(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<List<Profile>> search(String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Boolean> isFollowing(String userId1, String userId2) {
		// TODO Auto-generated method stub
		return null;
	}

}
