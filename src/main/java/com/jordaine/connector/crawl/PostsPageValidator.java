package com.jordaine.connector.crawl;

import com.jordaine.connector.error.NonRetryableConnectorException;
import com.jordaine.connector.error.RetryableConnectorException;
import com.jordaine.connector.model.PostsResponse;

import java.util.List;

/**
 * Validates that a fetched posts page is safe to process.
 *
 * <p>The crawler uses this guard to reject inconsistent pagination metadata before the sink or
 * checkpoint state can be updated.
 */
public class PostsPageValidator {

    /**
     * Validates one posts page against the originally requested pagination arguments.
     */
    public void validate(int requestedSkip, int requestedLimit, PostsResponse response)
            throws RetryableConnectorException, NonRetryableConnectorException {
        if (requestedSkip < 0) {
            throw new NonRetryableConnectorException("requestedSkip must be >= 0");
        }

        if (requestedLimit <= 0) {
            throw new NonRetryableConnectorException("requestedLimit must be > 0");
        }

        if (response == null) {
            throw new RetryableConnectorException("Received null response from posts API");
        }

        if (response.getTotal() < 0) {
            throw new RetryableConnectorException("Received negative total from posts API");
        }

        if (response.getSkip() != requestedSkip) {
            throw new RetryableConnectorException(
                    "Unexpected skip in response. expected=" + requestedSkip + ", actual=" + response.getSkip()
            );
        }

        if (response.getLimit() <= 0) {
            throw new RetryableConnectorException("Received invalid page limit from posts API");
        }

        List<?> posts = response.getPosts();
        if (posts == null) {
            throw new RetryableConnectorException("Received null posts list from posts API");
        }

        if (posts.size() > requestedLimit) {
            throw new RetryableConnectorException(
                    "Received more posts than requested. requestedLimit="
                            + requestedLimit
                            + ", actualCount="
                            + posts.size()
            );
        }

        if (posts.isEmpty() && requestedSkip < response.getTotal()) {
            throw new RetryableConnectorException(
                    "Received empty page before reaching total. skip="
                            + requestedSkip
                            + ", total="
                            + response.getTotal()
            );
        }
    }
}
