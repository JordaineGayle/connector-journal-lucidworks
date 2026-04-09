package com.jordaine.connector.transform;

import com.jordaine.connector.model.ConnectorDocument;
import com.jordaine.connector.model.DummyJsonPost;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class PostDocumentMapper {

    public ConnectorDocument map(DummyJsonPost post, String baseUrl) {
        ConnectorDocument document = new ConnectorDocument();
        document.setId("post-" + post.getId());
        document.setSourceType("post");
        document.setSourceId(post.getId());
        document.setTitle(post.getTitle());
        document.setBody(post.getBody());
        document.setTags(post.getTags());
        document.setAuthorId(post.getUserId());
        document.setReactionCount(post.getReactionCount());
        document.setViewCount(post.getViews());
        document.setSearchText(buildSearchText(post));
        document.setSourceUrl(buildSourceUrl(baseUrl, post.getId()));
        document.setFetchedAt(Instant.now());
        return document;
    }

    private String buildSearchText(DummyJsonPost post) {
        List<String> parts = new ArrayList<>();

        if (post.getTitle() != null && !post.getTitle().isBlank()) {
            parts.add(post.getTitle());
        }

        if (post.getBody() != null && !post.getBody().isBlank()) {
            parts.add(post.getBody());
        }

        if (post.getTags() != null && !post.getTags().isEmpty()) {
            StringJoiner joiner = new StringJoiner(" ");
            for (String tag : post.getTags()) {
                if (tag != null && !tag.isBlank()) {
                    joiner.add(tag);
                }
            }
            String tagsText = joiner.toString();
            if (!tagsText.isBlank()) {
                parts.add(tagsText);
            }
        }

        return String.join(" ", parts);
    }

    private String buildSourceUrl(String baseUrl, int postId) {
        return baseUrl.endsWith("/")
                ? baseUrl + "posts/" + postId
                : baseUrl + "/posts/" + postId;
    }
}