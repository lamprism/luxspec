package com.lamprism.luxspec.resource;

import com.lamprism.luxspec.data.QueryCriteria;
import com.lamprism.luxspec.data.QueryResult;
import com.lamprism.luxspec.data.QueryWindow;

/**
 * An optional resource-provider capability for ordinary structured collection browsing.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public interface ResourceBrowser<ID> extends ResourceProvider<ID> {
    /**
     * Browses resources using validated structured criteria and an explicit result window.
     *
     * @param criteria the validated filters and ordering
     * @param window the allowed result window
     * @return the resource query result
     */
    QueryResult<? extends Resource<ID>> browse(QueryCriteria criteria, QueryWindow window);
}
