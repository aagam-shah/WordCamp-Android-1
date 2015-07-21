package org.wordcamp.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.wordcamp.android.adapters.CacheFragmentStatePagerAdapter;
import org.wordcamp.android.db.DBCommunicator;
import org.wordcamp.android.networking.WPAPIClient;
import org.wordcamp.android.objects.WordCampDB;
import org.wordcamp.android.objects.wordcamp.WordCampNew;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity implements UpcomingWCFragment.upcomingFragListener,
        SearchView.OnQueryTextListener {

    private WCPagerAdapter mPagerAdapter;
    private String lastscanned;

    public DBCommunicator communicator;

    public List<WordCampDB> wordCampsList;
    private WPAPIClient wpapiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        wpapiClient = new WPAPIClient();
        setupUI();
        getSupportActionBar().setTitle(R.string.main_action_bar_title);
    }

    private void setupUI() {
        communicator = new DBCommunicator(this);
        communicator.start();
        wordCampsList = communicator.getAllWc();
        ViewCompat.setElevation(findViewById(R.id.header), getResources().getDimension(R.dimen.toolbar_elevation));
        mPagerAdapter = new WCPagerAdapter(getSupportFragmentManager(), this);
        ViewPager mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(2);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setTabTextColors(getResources().getColor(R.color.tab_normal_text),
                getResources().getColor(R.color.tab_selected_text));
        tabLayout.setupWithViewPager(mPager);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                getUpcomingFragment().startRefresh();
                break;
            case R.id.action_feedback:
                Intent feedbackIntent = new Intent(this, FeedbackActivity.class);
                startActivity(feedbackIntent);
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_base_act, menu);
        MenuItem searchItem = menu.findItem(R.id.search_wc);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(Html.fromHtml("<font color = #ffffff>"
                + getString(R.string.search_wc_hint) + "</font>"));
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        new Thread(new Runnable() {
            @Override
            public void run() {
                wpapiClient.cancelAllRequests();
            }
        }).start();

        if (communicator != null) {
            communicator.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (communicator == null) {
            communicator = new DBCommunicator(this);
            communicator.start();
            refreshAllFragmentsData();
        } else {
            communicator.start();
            refreshAllFragmentsData();
        }
    }

    private void fetchWCList() {

        final SharedPreferences pref = getSharedPreferences("wc", Context.MODE_PRIVATE);

        wpapiClient.getWordCampsList(this, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error ", Toast.LENGTH_SHORT).show();
                        stopRefresh();
                    }
                });
            }

            @Override
            public void onResponse(Response response) {
                SharedPreferences.Editor editor = pref.edit();

                wordCampsList = new ArrayList<>();
                Gson gson = new Gson();
                JSONArray array = null;
                try {
                    array = new JSONArray(response.body().string());


                    for (int i = 0; i < array.length(); i++) {
                        try {
                            WordCampNew wcs = gson.fromJson(array.getJSONObject(i).toString(), WordCampNew.class);
                            if (i == 0) {
                                //Set last scan date of WC List by saving the later WC's modified GMT date
                                editor.putString("date", wcs.getModifiedGmt());
                                lastscanned = wcs.getModifiedGmt();
                                editor.apply();
                            }
                            WordCampDB wordCampDB = new WordCampDB(wcs, lastscanned);
                            if (!wordCampDB.getWc_start_date().isEmpty()) {
                                wordCampsList.add(wordCampDB);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    communicator.addAllNewWC(wordCampsList);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshAllFragmentsData();
                            stopRefresh();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void stopRefresh() {
        getUpcomingFragment().stopRefresh();
        getMyWCFragment().stopRefresh();
    }

    private void refreshAllFragmentsData() {
        UpcomingWCFragment upcomingFragment = getUpcomingFragment();
        MyWCFragment myWCFragment = getMyWCFragment();
        wordCampsList = communicator.getAllWc();
        if (upcomingFragment != null) {
            upcomingFragment.updateList(wordCampsList);
        }
        if (myWCFragment != null) {
            myWCFragment.updateList(wordCampsList);
        }
    }

    public void refreshUpcomingFrag() {
        UpcomingWCFragment upcomingFragment = getUpcomingFragment();
        wordCampsList = communicator.getAllWc();
        if (upcomingFragment != null) {
            upcomingFragment.updateList(wordCampsList);
        }
    }

    private UpcomingWCFragment getUpcomingFragment() {
        return (UpcomingWCFragment) mPagerAdapter.getItemAt(0);
    }

    private MyWCFragment getMyWCFragment() {
        return (MyWCFragment) mPagerAdapter.getItemAt(1);
    }

    @Override
    public void onNewMyWCAdded(WordCampDB wordCampDB) {
        getMyWCFragment().addWC(wordCampDB);
    }

    @Override
    public void onRefreshStart() {
        fetchWCList();
    }

    @Override
    public void onMyWCRemoved(WordCampDB wordCampDB) {
        getMyWCFragment().removeSingleMYWC(wordCampDB);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        getUpcomingFragment().adapter.getFilter().filter(newText);
        getMyWCFragment().adapter.getFilter().filter(newText);
        return true;
    }

    private static class WCPagerAdapter extends CacheFragmentStatePagerAdapter {

        private Context mContext;

        public WCPagerAdapter(FragmentManager fm, Context ctx) {
            super(fm);
            mContext = ctx;
        }

        @Override
        protected Fragment createItem(int position) {
            switch (position) {
                case 1:
                    return MyWCFragment.newInstance();
                case 0:
                default:
                    return UpcomingWCFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return mContext.getString(R.string.my_wc_title);
                case 0:
                default:
                    return mContext.getString(R.string.upcoming_wc_title);
            }
        }
    }
}
