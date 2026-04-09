package com.jordaine.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DummyJsonPost {
    private int id;
    private String title;
    private String body;
    private List<String> tags;
    private int reactionCount;
    private int views;
    private int userId;

    public DummyJsonPost() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getReactionCount() {
        return reactionCount;
    }

    @JsonProperty("reactions")
    public void setReactions(Object reactions) {
        if (reactions == null) {
            this.reactionCount = 0;
            return;
        }

        if (reactions instanceof Number number) {
            this.reactionCount = number.intValue();
            return;
        }

        if (reactions instanceof Map<?, ?> map) {
            int likes = toInt(map.get("likes"));
            int dislikes = toInt(map.get("dislikes"));
            this.reactionCount = likes + dislikes;
            return;
        }

        this.reactionCount = 0;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}