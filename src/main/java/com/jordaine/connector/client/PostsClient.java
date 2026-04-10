package com.jordaine.connector.client;

import com.jordaine.connector.model.PostsResponse;

/**
 * Source access contract for paginated post retrieval.
 */
public interface PostsClient {
    /**
     * Fetches one page of posts using offset-based pagination.
     *
     * @param limit maximum number of posts requested for the page
     * @param skip zero-based number of posts to skip before reading this page
     * @return the upstream page payload
     */
    PostsResponse fetchPosts(int limit, int skip) throws Exception;
}
