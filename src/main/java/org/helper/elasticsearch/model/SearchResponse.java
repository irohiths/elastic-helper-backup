package org.helper.elasticsearch.model;

import org.json.JSONObject;

public class SearchResponse {
    public JSONObject searchResults;
    public Integer totalHitsCount;

    public JSONObject getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(JSONObject searchResults) {
        this.searchResults = searchResults;
    }

    public Integer getTotalHitsCount() {
        return totalHitsCount;
    }

    public void setTotalHitsCount(Integer totalHitsCount) {
        this.totalHitsCount = totalHitsCount;
    }
}
