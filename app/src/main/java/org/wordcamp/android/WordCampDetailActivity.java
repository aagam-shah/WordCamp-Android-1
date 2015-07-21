package org.wordcamp.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordcamp.android.adapters.WCDetailAdapter;
import org.wordcamp.android.db.DBCommunicator;
import org.wordcamp.android.networking.WPAPIClient;
import org.wordcamp.android.objects.WordCampDB;
import org.wordcamp.android.objects.speaker.Session;
import org.wordcamp.android.objects.speaker.SpeakerNew;
import org.wordcamp.android.objects.speaker.Terms;
import org.wordcamp.android.objects.wordcamp.WordCampNew;
import org.wordcamp.android.utils.CustomGsonDeSerializer;
import org.wordcamp.android.wcdetails.MySessionsActivity;
import org.wordcamp.android.wcdetails.SessionsFragment;
import org.wordcamp.android.wcdetails.SpeakerFragment;
import org.wordcamp.android.wcdetails.WordCampOverview;

import java.io.IOException;

/**
 * Created by aagam on 26/1/15.
 */
public class WordCampDetailActivity extends AppCompatActivity implements SessionsFragment.SessionFragmentListener,
        SpeakerFragment.SpeakerFragmentListener, WordCampOverview.WordCampOverviewListener {

    private WCDetailAdapter adapter;
    private Toolbar toolbar;
    public WordCampDB wcdb;
    public int wcid;
    public DBCommunicator communicator;
    private WPAPIClient wpapiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wcdb = (WordCampDB) getIntent().getSerializableExtra("wc");
        wcid = wcdb.getWc_id();
        setContentView(R.layout.activity_wordcamp_detail);
        wpapiClient = new WPAPIClient();
        communicator = new DBCommunicator(this);
        communicator.start();
        initGUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        communicator.close();
    }

    private void initGUI() {
        ViewCompat.setElevation(findViewById(R.id.header), getResources().getDimension(R.dimen.toolbar_elevation));
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        adapter = new WCDetailAdapter(getSupportFragmentManager(), this);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setTabTextColors(getResources().getColor(R.color.tab_normal_text),
                getResources().getColor(R.color.tab_selected_text));
        tabLayout.setupWithViewPager(pager);

        setToolbar();
    }

    private void setToolbar() {
        toolbar.setTitle(wcdb.getWc_title());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateWordCampData();
                break;
            case R.id.item_menu_feedback:
                String url = communicator.getFeedbackUrl(wcid);
                if (url == null) {
                    Toast.makeText(this, getString(R.string.feedback_url_not_available_toast),
                            Toast.LENGTH_LONG).show();
                } else {
                    startWebIntent(url);
                }
                break;
            case android.R.id.home:
                finish();
                break;
            case R.id.item_menu_website:
                break;
        }

        return true;
    }

    private void startMySessionActivity() {
        Intent mySessionIntent = new Intent(this, MySessionsActivity.class);
        mySessionIntent.putExtra("wcid", wcid);
        startActivity(mySessionIntent);
    }

    private void startWebIntent(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(browserIntent);
    }

    private void updateWordCampData() {
        String webURL = wcdb.getUrl();

        fetchSpeakersAPI(webURL);
        getSessionsFragment().startRefreshSession();
//        fetchSessionsAPI(webURL);
//        fetchOverviewAPI();
    }

    private void fetchOverviewAPI() {
        wpapiClient.getSingleWC(this, wcid, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        WordCampOverview overview = getOverViewFragment();
                        if (overview != null) {
                            overview.stopRefreshOverview();
                        }
                    }
                });
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    JSONObject object = new JSONObject(response.body().string());
                    Gson g = new Gson();
                    WordCampNew wc = g.fromJson(object.toString(), WordCampNew.class);
                    final WordCampDB wordCampDB = new WordCampDB(wc, "");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            communicator.updateWC(wordCampDB);
                            WordCampOverview overview = getOverViewFragment();
                            if (overview != null) {
                                overview.updateData(wordCampDB);
                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.update_overview_toast), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });


    }

    private void fetchSessionsAPI(String webURL) {
        wpapiClient.getWordCampSchedule(this, webURL, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                if (e == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopRefreshSession();
                        }
                    });
                }
            }

            @Override
            public void onResponse(Response response) throws IOException {

                if (response.isSuccessful()) {
                    try {
                        final JSONArray array = new JSONArray(response.body().string());
                        Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(Terms.class,
                                new CustomGsonDeSerializer()).create();

                        for (int i = 0; i < array.length(); i++) {
                            try {
                                Session session = gson.fromJson(array.getJSONObject(i).toString(), Session.class);
                                if (communicator != null) {
                                    communicator.addSession(session, wcid);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), getString(R.string.update_sessions_toast),
                                        Toast.LENGTH_SHORT).show();
                                stopRefreshSession();
                                if (array.length() > 0) {
                                    updateSessionContent();
                                }
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopRefreshSession();
                        }
                    });
                }
            }

        });

    }

    private void stopRefreshSession() {
        SessionsFragment fragment = getSessionsFragment();
        if (fragment != null) {
            fragment.stopRefreshSession();
        }
    }

    private void stopRefreshSpeaker() {
        SpeakerFragment fragment = getSpeakerFragment();
        if (fragment != null) {
            fragment.stopRefreshSpeaker();
        }

        updateSessionContent();
    }

    private void updateSessionContent() {
        SessionsFragment fragment = getSessionsFragment();
        if (fragment != null) {
            fragment.updateData();
        }
    }

    private void fetchSpeakersAPI(String webURL) {
        wpapiClient.getWordCampSpeakers(this, webURL, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

                if (e == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.no_network_toast),
                                    Toast.LENGTH_SHORT).show();
                            stopRefreshSpeaker();
                        }
                    });
                }
            }

            @Override
            public void onResponse(Response response) {

                if (response.isSuccessful()) {
                    try {
                        final JSONArray array = new JSONArray(response.body().string());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addUpdateSpeakers(array);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.no_network_toast),
                                    Toast.LENGTH_SHORT).show();
                            stopRefreshSpeaker();
                        }
                    });
                }
            }
        });
    }

    private void addUpdateSpeakers(JSONArray array) {
        Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(Terms.class,
                new CustomGsonDeSerializer()).create();
        for (int i = 0; i < array.length(); i++) {
            try {
                SpeakerNew skn = gson.fromJson(array.getJSONObject(i).toString(), SpeakerNew.class);
                communicator.addSpeaker(skn, wcid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (array.length() > 0) {
            SpeakerFragment fragment = getSpeakerFragment();
            if (fragment != null) {
                fragment.updateSpeakers(communicator.getAllSpeakers(wcid));
            }
        }
        Toast.makeText(getApplicationContext(), getString(R.string.update_speakers_toast), Toast.LENGTH_SHORT).show();
        stopRefreshSpeaker();
    }

    private SpeakerFragment getSpeakerFragment() {
        return (SpeakerFragment) adapter.getItemAt(2);
    }

    private SessionsFragment getSessionsFragment() {
        return (SessionsFragment) adapter.getItemAt(1);
    }

    private WordCampOverview getOverViewFragment() {
        return (WordCampOverview) adapter.getItemAt(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wc_detail, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (communicator == null) {
            communicator = new DBCommunicator(this);
        } else {
            communicator.restart();
            updateSessionContent();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        new Thread(new Runnable() {
            @Override
            public void run() {
                wpapiClient.cancelAllRequests();
            }
        }).start();
        if (communicator != null)
            communicator.close();
    }

    @Override
    protected void onStop() {
        super.onStop();
        new Thread(new Runnable() {
            @Override
            public void run() {
                wpapiClient.cancelAllRequests();
            }
        }).start();
    }

    @Override
    public void startRefreshSessions() {
        //Even we are refreshing sessions,
        // we will fetch Speakers as we get Sessions from there

        String webURL = wcdb.getUrl();
        fetchSessionsAPI(webURL);
        getSessionsFragment().startRefreshingBar();
    }

    @Override
    public void startRefreshSpeakers() {
        String webURL = wcdb.getUrl();
        fetchSpeakersAPI(webURL);
    }

    @Override
    public void refreshOverview() {
        fetchOverviewAPI();
    }
}
