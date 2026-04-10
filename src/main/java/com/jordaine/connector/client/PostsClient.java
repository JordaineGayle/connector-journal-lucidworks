package com.jordaine.connector.client;

import com.jordaine.connector.model.PostsResponse;

public interface PostsClient {
    PostsResponse fetchPosts(int limit, int skip) throws Exception;
}
