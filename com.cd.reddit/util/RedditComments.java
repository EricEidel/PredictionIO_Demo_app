package util;

import java.util.List;

import mapping.RedditComment;
import mapping.RedditLink;
import mapping.RedditMore;

public class RedditComments 
{
	private final RedditLink parentLink;
	private final List<RedditComment> comments;
	private final RedditMore more;
	
	public RedditComments(final RedditLink theParentLink,
						  final List<RedditComment> theParsedTypes, 
						  final RedditMore theMore){
		parentLink = theParentLink;
		comments = theParsedTypes;
		more = theMore;
	}

	public RedditLink getParentLink(){
		return parentLink;
	}
	
	public List<RedditComment> getComments() {
		return comments;
	}

	public RedditMore getMore() {
		return more;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RedditComments [parentLink=");
		builder.append(parentLink);
		builder.append(", comments=");
		builder.append(comments);
		builder.append(", more=");
		builder.append(more);
		builder.append("]");
		return builder.toString();
	}
}
