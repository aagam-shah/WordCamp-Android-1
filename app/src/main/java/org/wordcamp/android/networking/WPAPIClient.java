package org.wordcamp.android.networking;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.wordcamp.android.BuildConfig;

/**
 * Created by aagam on 14/1/15.
 */
public class WPAPIClient {

    private static final String QUERY_PARAM_SPEAKERS = "wp-json/posts?type=wcb_speaker&filter[posts_per_page]=100";

    private static final String QUERY_PARAM_WORDCAMP_LIST = "wp-json/posts?type=wordcamp&filter[order]=DESC&filter[posts_per_page]=50";

    private static final String QUERY_PARAM_SCHEDULE = "wp-json/posts?type=wcb_session&filter[order]=DESC&filter[orderby]=modified&filter[posts_per_page]=100";

    private static final String QUERY_PARAM_SINGLEWC = "wp-json/posts/";

    private OkHttpClient okHttpClient;

    public WPAPIClient() {
        okHttpClient = new OkHttpClient();
    }

    private String normalizeWordCampUrl(String wordcampURL) {
        if (!wordcampURL.endsWith("/")) {
            wordcampURL = wordcampURL + "/";
        }
        return wordcampURL;
    }

    public void getWordCampsList(Context context, Callback callback) {
        okHttpClient.newCall(generateRequest(context,
                BuildConfig.CENTRAL_WORDCAMP_URL + QUERY_PARAM_WORDCAMP_LIST)).enqueue(callback);
    }

    public void getWordCampSpeakers(Context ctx, String wordcampURL, Callback callback) {
        okHttpClient.newCall(generateRequest(ctx,
                normalizeWordCampUrl(wordcampURL) + QUERY_PARAM_SPEAKERS)).enqueue(callback);
    }

    public void getWordCampSchedule(Context ctx, String wordcampURL, Callback callback) {
        okHttpClient.newCall(generateRequest(ctx,
                normalizeWordCampUrl(wordcampURL) + QUERY_PARAM_SCHEDULE)).enqueue(callback);
    }

    public void getSingleWC(Context ctx, int wcid, Callback callback) {
        okHttpClient.newCall(generateRequest(ctx,
                BuildConfig.CENTRAL_WORDCAMP_URL + QUERY_PARAM_SINGLEWC + wcid)).enqueue(callback);
    }

    private Request generateRequest(Context context, String url) {
        return new Request.Builder()
                .url(url)
                .tag("wp")
                .build();
    }

    public void cancelAllRequests() {
        okHttpClient.cancel("wp");

    }
}
