package com.jordaine.connector.transform;

import com.jordaine.connector.model.ConnectorDocument;
import com.jordaine.connector.model.DummyJsonPost;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class PostDocumentMapper implements DocumentMapper<DummyJsonPost, ConnectorDocument> {
    private final String sourceUrlPrefix;

    public PostDocumentMapper(String baseUrl, String postsEndpoint) {
        this.sourceUrlPrefix = normalizeBaseUrl(baseUrl) + normalizeEndpoint(postsEndpoint);
    }

    @Override
    public ConnectorDocument map(DummyJsonPost post) {
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
        document.setSourceUrl(buildSourceUrl(post.getId()));
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

    private String buildSourceUrl(int postId) {
        return sourceUrlPrefix + "/" + postId;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }

        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("postsEndpoint must not be blank");
        }

        String normalizedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return normalizedEndpoint.endsWith("/")
                ? normalizedEndpoint.substring(0, normalizedEndpoint.length() - 1)
                : normalizedEndpoint;
    }
}