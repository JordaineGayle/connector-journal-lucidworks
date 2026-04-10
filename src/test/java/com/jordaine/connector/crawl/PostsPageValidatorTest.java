package com.jordaine.connector.crawl;

import com.jordaine.connector.error.RetryableConnectorException;
import com.jordaine.connector.model.DummyJsonPost;
import com.jordaine.connector.model.PostsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostsPageValidatorTest {

    @Test
    void shouldAcceptConsistentPage() {
        DummyJsonPost post = new DummyJsonPost();
        post.setId(1);

        PostsResponse response = new PostsResponse();
        response.setPosts(List.of(post));
        response.setSkip(0);
        response.setLimit(10);
        response.setTotal(1);

        PostsPageValidator validator = new PostsPageValidator();
        assertDoesNotThrow(() -> validator.validate(0, 10, response));
    }

    @Test
    void shouldRejectSkipMismatch() {
        PostsResponse response = new PostsResponse();
        response.setPosts(List.of());
        response.setSkip(5);
        response.setLimit(10);
        response.setTotal(10);

        PostsPageValidator validator = new PostsPageValidator();
        assertThrows(RetryableConnectorException.class, () -> validator.validate(0, 10, response));
    }
}
