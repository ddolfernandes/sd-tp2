package microgram.impl.mongo;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;

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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;

import microgram.api.Profile;
import microgram.api.UserFollowRelation;
import microgram.api.java.Profiles;
import microgram.api.java.Result;

public class MongoProfiles implements Profiles {

	// melhor maneira de modelar os followers e following?

	private MongoDatabase db;

	private static final String MONGO_HOSTNAME = "mongodb://mongo1,mongo2,mongo3";
	
	private static final String DB_NAME = "mongoDataBase";
	private static final String DB_USERS_TABLE = "Users";
	private static final String DB_FOLLOWERS_TABLE = "Followers";
	private static final String DB_FOLLOWING_TABLE = "Following";
	private static final String USERID = "userId";
	private static final String USERID2 = "userId2";

	MongoCollection<Profile> usersCol;
	MongoCollection<UserFollowRelation> followersCol; // userId esta a seguir userId2

	public MongoProfiles() throws UnknownHostException {
		MongoClient mongo = new MongoClient(MONGO_HOSTNAME);

		// e suposto ter CodecRegistries antes do fromRegistries e do fromProviders?
		CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

		// nome arbitrario para a database?
		db = mongo.getDatabase(DB_NAME).withCodecRegistry(pojoCodecRegistry);
		// basta ir buscar a collection no construtor? -> bson propertys do Profile,

		usersCol = db.getCollection(DB_USERS_TABLE, Profile.class);

		usersCol.createIndex(Indexes.ascending(USERID), new IndexOptions().unique(true));

		followersCol = db.getCollection(DB_USERS_TABLE, UserFollowRelation.class);
		followersCol.createIndex(Indexes.ascending(USERID, USERID2), new IndexOptions().unique(true)); // nao preciso
																										// ter duas
																										// tabelas,
																										// apenas uma
																										// que guarda
																										// ambos, com
		// pq preciso de um index no USERID2?

	}

	/*
	 * retorna com estatisticas certas
	 * 
	 */
	@Override
	public Result<Profile> getProfile(String userId) {

		Profile profile = usersCol.find(Filters.eq(USERID, userId)).first();

		if (profile == null)
			return error(NOT_FOUND);

		int following = (int) usersCol.countDocuments(Filters.eq(USERID, userId)); // assumes number of followers doesnt
																					// exceed an integer
		int followers = (int) usersCol.countDocuments(Filters.eq(USERID2, userId));

		profile.setFollowers(followers);
		profile.setFollowing(following);
		return ok(profile);

	}

	@Override
	public Result<Void> createProfile(Profile profile) {

		try {
			usersCol.insertOne(profile);
			return ok();
		} catch (MongoWriteException x) {
			return error(CONFLICT);
		}
	}

	@Override
	public Result<Void> deleteProfile(String userId) {

		DeleteResult res = usersCol.deleteOne(Filters.eq(USERID, userId));

		if (!res.wasAcknowledged())
			return error(NOT_FOUND);

		followersCol.deleteMany(Filters.eq(USERID, userId)); // apaga quem nos estamos a seguir
		followersCol.deleteMany(Filters.eq(USERID2, userId)); // apaga quem nos esta a seguir

		return ok();
	}

	
	@Override
	public Result<List<Profile>> search(String prefix) { //regex do search?
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing) {
		if (!profileExists(userId1) || !profileExists(userId2))
			return error(NOT_FOUND);
		
		return null;

	}

	@Override
	public Result<Boolean> isFollowing(String userId1, String userId2) {
		if (!profileExists(userId1) || !profileExists(userId2))
			return error(NOT_FOUND);

		boolean isFollowing = followersCol
				.countDocuments(Filters.and(Filters.eq(USERID, userId1), Filters.eq(USERID2, userId2))) != 0;

		return ok(isFollowing);
	}

	/*
	 * metodo auxiliar para verificar se o perfil com userId existe
	 */
	private boolean profileExists(String userId) {
		return usersCol.find(Filters.eq(USERID, userId)).first() != null;
	}

}
