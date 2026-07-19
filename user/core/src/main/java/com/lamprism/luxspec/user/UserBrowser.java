package com.lamprism.luxspec.user;

import com.lamprism.luxspec.data.QueryCriteria;
import com.lamprism.luxspec.data.QueryResult;
import com.lamprism.luxspec.data.QueryWindow;
import com.lamprism.luxspec.resource.ResourceBrowser;

/**
 * Browses users through ordinary typed structured queries.
 *
 * @author RollW
 */
public interface UserBrowser extends ResourceBrowser<Long> {
    /**
     * Returns a user query result for validated criteria and an allowed window.
     *
     * @param criteria the typed user filters and ordering
     * @param window the allowed result window
     * @return the user query result
     */
    @Override
    QueryResult<User> browse(QueryCriteria criteria, QueryWindow window);
}
