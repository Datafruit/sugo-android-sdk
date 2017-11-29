/**
 * This package contains the interface to Sugo that you can use from your
 * Android apps. You can use Sugo to send events, update people analytics properties,
 * display push notifications and other Sugo-driven content to your users.
 *
 * The primary interface to Sugo services is in {@link io.sugo.android.mpmetrics.SugoAPI}.
 * At it's simplest, you can send events with
 * <pre>
 * {@code
 *
 * SugoAPI sugo = SugoAPI.getInstance(context, SUGO_TOKEN);
 * sugo.track("Library integrated", null);
 *
 * }
 * </pre>
 *
 * In addition to this reference documentation, you can also see our overview
 * and getting started documentation at
 * <a href="https://sugo.com/help/reference/android" target="_blank"
 *    >https://sugo.com/help/reference/android</a>
 *
 */
package io.sugo.android.mpmetrics;