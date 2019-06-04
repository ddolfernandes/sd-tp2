package microgram.impl.mongo;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.MongoClient;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;

import microgram.api.Post;
import microgram.api.PostLikesRelation;
import microgram.api.Profile;
import microgram.api.UserFollowRelation;
import microgram.api.java.Posts;
import microgram.api.java.Result;

public class MongoPosts implements Posts {

	private MongoDatabase db;

	private static final String MONGO_HOSTNAME = "mongo1";

	private static final String DB_NAME = "mongoDataBase";
	private static final String DB_POSTS_TABLE = "Posts";
	private static final String DB_LIKES_TABLE = "Likes";
	private static final String DB_USERS_TABLE = "Users";
	private static final String DB_FOLLOWERS_TABLE = "Followers";
	private static final String USERID = "userId";
	private static final String POSTID = "postId";
	private static final String OWNERID = "ownerId";

	MongoCollection<Post> postsCol;
	MongoCollection<PostLikesRelation> likesCol; // postId com os gostos de varios userId's
	MongoCollection<Profile> usersCol;
	MongoCollection<UserFollowRelation> followersCol; // userId esta a seguir userId2

	public MongoPosts() throws UnknownHostException {
		MongoClient mongo = new MongoClient(MONGO_HOSTNAME);

		CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

		db = mongo.getDatabase(DB_NAME).withCodecRegistry(pojoCodecRegistry);
		postsCol = db.getCollection(DB_POSTS_TABLE, Post.class);
		postsCol.createIndex(Indexes.ascending(USERID), new IndexOptions().unique(true));
		postsCol.createIndex(Indexes.hashed(OWNERID));

		likesCol = db.getCollection(DB_LIKES_TABLE, PostLikesRelation.class);
		likesCol.createIndex(Indexes.ascending(POSTID, USERID), new IndexOptions().unique(true));
		likesCol.createIndex(Indexes.hashed(USERID));

		usersCol = db.getCollection(DB_USERS_TABLE, Profile.class);
		followersCol = db.getCollection(DB_FOLLOWERS_TABLE, UserFollowRelation.class);

	}

	@Override
	public Result<Post> getPost(String postId) {
		Post post = postsCol.find(Filters.eq(POSTID, postId)).first();

		if (post == null)
			return error(NOT_FOUND);

		int likes = (int) likesCol.countDocuments(Filters.eq(POSTID, postId));

		post.setLikes(likes);

		return ok(post);
	}

	@Override
	public Result<String> createPost(Post post) {
		try {
			postsCol.insertOne(post);
			return ok();
		} catch (MongoWriteException x) {
			return error(CONFLICT);
		}
	}

	@Override
	public Result<Void> deletePost(String postId) {
		DeleteResult res = postsCol.deleteOne(Filters.eq(POSTID, postId));

		if (!res.wasAcknowledged())
			return error(NOT_FOUND);

		likesCol.deleteMany(Filters.eq(POSTID, postId)); // apaga todos os likes do post

		return ok();
	}

	@Override
	public Result<Void> like(String postId, String userId, boolean isLiked) {
		if (!profileExists(userId) || !postExists(postId))
			return error(NOT_FOUND);

		if (isLiked) {
			PostLikesRelation temp = new PostLikesRelation(postId, userId); // adiciona entrada representando um gosto
																			// do userId ao postId
			likesCol.insertOne(temp);

		} else { // remover
			likesCol.deleteOne(Filters.and(Filters.eq(USERID, postId), Filters.eq(USERID, userId)));

		}

		return ok();
	}

	@Override
	public Result<Boolean> isLiked(String postId, String userId) {
		if (!profileExists(userId) || !postExists(postId))
			return error(NOT_FOUND);

		boolean isLiked = likesCol
				.countDocuments(Filters.and(Filters.eq(POSTID, postId), Filters.eq(USERID, userId))) != 0;

		return ok(isLiked);
	}

	@Override
	public Result<List<String>> getPosts(String userId) {
		if (!profileExists(userId))
			return error(NOT_FOUND);

		// ele existe, logo vamos buscar os followers dele
		
		List<String> feed = new ArrayList<>();
		
		
		MongoCursor<Post> cursor =  postsCol.find(Filters.eq(OWNERID, userId)).iterator();
		while(cursor.hasNext()) {
			feed.add(cursor.next().getPostId());
		}
		
		/*postsCol.find(Filters.eq(OWNERID, userId)).forEach((Post doc) -> {	

			feed.add(doc.getPostId());
		});;
		*/
		return ok(feed);
	}

	@Override
	public Result<List<String>> getFeed(String userId) {
		if (!profileExists(userId))
			return error(NOT_FOUND);

		// ele existe, logo vamos buscar os followers dele
		List<String> feed = new ArrayList<>();
		
		MongoCursor<UserFollowRelation> cursor = followersCol.find(Filters.eq(USERID, userId)).iterator();
		
		while(cursor.hasNext()) {
			
			UserFollowRelation doc = cursor.next();
			MongoCursor<Post> cursor2 = postsCol.find(Filters.eq(OWNERID, doc.getUserId2())).iterator();
			
			while(cursor2.hasNext()) {
				Post docc = cursor2.next();
				feed.add(docc.getPostId());
			}
			
		}
		/*
		followersCol.find(Filters.eq(USERID, userId)).forEach((UserFollowRelation doc) -> {  //para cada utilizador que userId esta a seguir

			postsCol.find(Filters.eq(OWNERID, doc.getUserId2())).forEach((Post docc) -> {	//para cada post de cada utilizador que o userId esta a seguir

				feed.add(docc.getPostId());
			});
		});*/

		return ok(feed);

	}

	/*
	 * metodo auxiliar para verificar se o perfil com userId existe
	 */
	private boolean profileExists(String userId) {
		return usersCol.find(Filters.eq(USERID, userId)).first() != null;
	}

	/*
	 * metodo auxiliar para verificar se o perfil com userId existe
	 */
	private boolean postExists(String postId) {
		return postsCol.find(Filters.eq(POSTID, postId)).first() != null;
	}

}
