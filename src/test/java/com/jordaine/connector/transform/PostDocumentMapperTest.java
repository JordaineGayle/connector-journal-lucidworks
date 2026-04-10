package com.jordaine.connector.transform;

import com.jordaine.connector.model.ConnectorDocument;
import com.jordaine.connector.model.DummyJsonPost;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests mapping from source post payloads into normalized connector documents.
 */
class PostDocumentMapperTest {

    @Test
    void shouldMapPostToConnectorDocument() {
        DummyJsonPost post = new DummyJsonPost();
        post.setId(1);
        post.setTitle("Test Title");
        post.setBody("Test Body");
        post.setTags(List.of("tag1", "tag2"));
        post.setUserId(42);
        post.setViews(100);

        post.setReactions(10); // triggers setter

        PostDocumentMapper mapper = new PostDocumentMapper("https://dummyjson.com", "/posts");

        ConnectorDocument doc = mapper.map(post);

        assertEquals("post-1", doc.getId());
        assertEquals("post", doc.getSourceType());
        assertEquals(1, doc.getSourceId());
        assertEquals("Test Title", doc.getTitle());
        assertEquals("Test Body", doc.getBody());
        assertEquals(42, doc.getAuthorId());
        assertEquals(10, doc.getReactionCount());
        assertEquals(100, doc.getViewCount());
        assertTrue(doc.getSearchText().contains("Test Title"));
        assertTrue(doc.getSearchText().contains("Test Body"));
        assertTrue(doc.getSearchText().contains("tag1"));
        assertNotNull(doc.getFetchedAt());
        assertEquals("https://dummyjson.com/posts/1", doc.getSourceUrl());
    }
}