package microgram.api;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class PostLikesRelation {

	private String postId;
	private String userId;

	@BsonCreator
	public PostLikesRelation(@BsonProperty("postId") String postId, @BsonProperty("userId") String userId) {

		this.setPostId(postId);
		this.setUserId(userId);

	}

	public PostLikesRelation() {

	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPostId() {
		return postId;
	}

	public void setPostId(String postId) {
		this.postId = postId;
	}

}
