package io.sugo.android.mpmetrics;

/**
 * A Tweak allows you to alter values in your user's applications through the Sugo UI.
 * Use Tweaks to expose parameters you can adjust in A/B tests, to determine what application
 * settings result in the best experiences for your users and which are best for achieving
 * your goals.
 *
 * You can declare tweaks with
 * {@link SugoAPI#stringTweak(String, String)}, {@link SugoAPI#booleanTweak(String, boolean)},
 * {@link SugoAPI#doubleTweak(String, double)}, {@link SugoAPI#longTweak(String, long)},
 * and other tweak-related interfaces on SugoAPI.
 */
public interface Tweak<T> {
    /**
     * @return a value for this tweak, either the default value or a value set as part of a Sugo A/B test.
     */
    T get();
}
