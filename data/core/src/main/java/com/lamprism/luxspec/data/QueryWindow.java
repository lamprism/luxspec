package com.lamprism.luxspec.data;

/**
 * Expresses the requested amount of a collection result.
 *
 * @author RollW
 */
public sealed interface QueryWindow permits UnboundedWindow, PageWindow, SliceWindow {
}
