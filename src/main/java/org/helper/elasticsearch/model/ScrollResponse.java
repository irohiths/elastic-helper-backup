package org.helper.elasticsearch.model;

import org.json.JSONObject;

public class ScrollResponse {
	public String scrollID;
	public JSONObject searchResults;
	public Integer totalHitsCount;
	public Integer getTotalHitsCount() {
		return totalHitsCount;
	}
	public void setTotalHitsCount(Integer totalHitsCount) {
		this.totalHitsCount = totalHitsCount;
	}
	public String getScrollID() {
		return scrollID;
	}
	public void setScrollID(String scrollID) {
		this.scrollID = scrollID;
	}
	public JSONObject getSearchResults() {
		return searchResults;
	}
	public void setSearchResults(JSONObject searchResults) {
		this.searchResults = searchResults;
	}
	
}
