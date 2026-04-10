package com.jordaine.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Source DTO for one paginated posts response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostsResponse {
    private List<DummyJsonPost> posts;
    private int total;
    private int skip;
    private int limit;

    public PostsResponse() {
    }

    public List<DummyJsonPost> getPosts() {
        return posts;
    }

    public void setPosts(List<DummyJsonPost> posts) {
        this.posts = posts;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSkip() {
        return skip;
    }

    public void setSkip(int skip) {
        this.skip = skip;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}