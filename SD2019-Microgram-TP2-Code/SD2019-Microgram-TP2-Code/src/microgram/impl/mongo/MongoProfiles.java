package microgram.impl.mongo;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;

import microgram.api.Post;
import microgram.api.Profile;
import microgram.api.UserFollowRelation;
import microgram.api.java.Profiles;
import microgram.api.java.Result;

public class MongoProfiles implements Profiles {

	private MongoDatabase db;

	//private static final String MONGO_HOSTNAME = "mongo1";
	
		private static final String[] hostnames = {"mongo1","mongo2","mongo3"};
		private static final ServerAddress MONGO1_HOSTNAME = new ServerAddress("mongo1");
		private static final ServerAddress MONGO2_HOSTNAME = new ServerAddress("mongo2");
		private static final ServerAddress MONGO3_HOSTNAME = new ServerAddress("mongo3");
		private static final ArrayList<ServerAddress> MONGO_HOSTNAME = new ArrayList<ServerAddress>() {
			{
				add(MONGO1_HOSTNAME);
				add(MONGO2_HOSTNAME);
				add(MONGO3_HOSTNAME);
				
			}
		};


	private static final String DB_NAME = "mongoDataBase";
	private static final String DB_USERS_TABLE = "Users";
	private static final String DB_FOLLOWERS_TABLE = "Followers";
	private static final String USERID = "userId";
	private static final String USERID2 = "userId2";
	private static final String OWNERID = "ownerId";
	private static final String POSTID = "postId";
	private static final String DB_POSTS_TABLE = "Posts";

	MongoCollection<Profile> usersCol;
	MongoCollection<UserFollowRelation> followersCol; // userId esta a seguir userId2
	MongoCollection<Post> postsCol;

	public MongoProfiles() throws UnknownHostException {
		MongoClient mongo = new MongoClient(MONGO_HOSTNAME);

		CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

		// nome arbitrario para a database?
		db = mongo.getDatabase(DB_NAME).withCodecRegistry(pojoCodecRegistry);
		usersCol = db.getCollection(DB_USERS_TABLE, Profile.class);
		usersCol.createIndex(Indexes.ascending(USERID), new IndexOptions().unique(true));

		followersCol = db.getCollection(DB_FOLLOWERS_TABLE, UserFollowRelation.class);
		followersCol.createIndex(Indexes.ascending(USERID, USERID2), new IndexOptions().unique(true)); // nao preciso
		followersCol.createIndex(Indexes.hashed(USERID2));

		postsCol = db.getCollection(DB_POSTS_TABLE, Post.class);

	}

	/*
	 * falta retornar as estatisticas dos posts, aka ir buscar tabela dos posts
	 * 
	 */
	@Override
	public Result<Profile> getProfile(String userId) {

		Profile profile = usersCol.find(Filters.eq(USERID, userId)).first();

		if (profile == null)
			return error(NOT_FOUND);

		int following = (int) followersCol.countDocuments(Filters.eq(USERID, userId)); // assumes number of followers doesnt
																					// exceed an integer
		int followers = (int) followersCol.countDocuments(Filters.eq(USERID2, userId));

		int posts = (int) postsCol.countDocuments(Filters.eq(OWNERID, userId));

		profile.setFollowers(followers);
		profile.setFollowing(following);
		profile.setPosts(posts);
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
		
		Profile profile = usersCol.find(Filters.eq(USERID, userId)).first();
		
		if(profile == null)
			return error(NOT_FOUND);

		DeleteResult res = usersCol.deleteOne(Filters.eq(USERID, userId));

		if (!res.wasAcknowledged())
			return error(NOT_FOUND);

		followersCol.deleteMany(Filters.eq(USERID, userId)); // apaga quem nos estamos a seguir
		followersCol.deleteMany(Filters.eq(USERID2, userId)); // apaga quem nos esta a seguir
		postsCol.deleteMany(Filters.eq(OWNERID, userId));

		return ok();
	}

	@Override
	public Result<List<Profile>> search(String prefix) { // regex do search?

		List<Profile> matches = new ArrayList<>();

		String regex = "^" + prefix + ".*$";

		try (MongoCursor<Profile> cursor = usersCol.find(Filters.regex(USERID, regex)).iterator()) {
			while (cursor.hasNext()) {
				Profile doc = cursor.next();
				matches.add(doc);
			}
		}
		/*
		 * usersCol.find(Filters.regex(USERID,regex )).forEach((Profile doc) -> {
		 * matches.add(doc); });;
		 */

		return ok(matches);
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing) {
		if (!profileExists(userId1) || !profileExists(userId2))
			return error(NOT_FOUND);
		
		UserFollowRelation exists = followersCol.find(Filters.and(Filters.eq(USERID,userId1),Filters.eq(USERID2, userId2))).first();
			

		if (isFollowing) { // adicionar
			if(exists != null)
				return error(CONFLICT);
						
			UserFollowRelation temp = new UserFollowRelation(userId1, userId2);
			followersCol.insertOne(temp);

		} else { // remover
			
			if(exists==null)
				return error(NOT_FOUND);
			
			followersCol.deleteOne(Filters.and(Filters.eq(USERID, userId1), Filters.eq(USERID2, userId2)));

		}

		return ok();

	}

	@Override
	public Result<Boolean> isFollowing(String userId1, String userId2) {
		if (!profileExists(userId1) || !profileExists(userId2))
			return error(NOT_FOUND);

		UserFollowRelation temp = followersCol
				.find(Filters.and(Filters.eq(USERID, userId1), Filters.eq(USERID2, userId2))).first();

		return ok(temp!=null);
	}

	/*
	 * metodo auxiliar para verificar se o perfil com userId existe
	 */
	private boolean profileExists(String userId) {
		return usersCol.find(Filters.eq(USERID, userId)).first() != null;
	}

}