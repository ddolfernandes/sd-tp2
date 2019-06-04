package microgram.api;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class UserFollowRelation {
	
	private String userId;
	private String userId2;
	
	
	

	@BsonCreator
	public UserFollowRelation(@BsonProperty("userId") String userId,@BsonProperty("userId2") String userId2) {
		
		this.setUserId(userId);
		this.setUserId2(userId2);
		
	}

	
	public UserFollowRelation() {
		// TODO Auto-generated constructor stub
	}


	//necessario??
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId2() {
		return userId2;
	}

	public void setUserId2(String userId2) {
		this.userId2 = userId2;
	}
	
	

}
