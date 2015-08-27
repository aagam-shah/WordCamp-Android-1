package org.wordcamp.android.wcdetails;

import android.animation.Animator;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.wordcamp.android.R;
import org.wordcamp.android.adapters.SpeakerDetailAdapter;
import org.wordcamp.android.db.DBCommunicator;
import org.wordcamp.android.objects.SessionDB;
import org.wordcamp.android.objects.SpeakerDB;
import org.wordcamp.android.objects.speaker.SpeakerNew;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by aagam on 26/2/15.
 */
public class SpeakerDetailsActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private SpeakerDB speakerDB;
    private SpeakerNew speaker;
    private Gson gson;
    private DBCommunicator communicator;
    private HashMap<String, Integer> titleSession;
    private TextView info,sessionTitle;
    private ListView lv;
    private Animator mCurrentAnimator;
    private ImageView dp;
    private ImageView zoomImageView;
    private AtomicBoolean visible = new AtomicBoolean(false);
    private float startScale;
    private View container;
    private int mShortAnimationDuration;
    private Rect startBounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        speakerDB = (SpeakerDB) getIntent().getSerializableExtra("speaker");
        setContentView(R.layout.activity_speaker_detail);

        gson = new Gson();
        communicator = new DBCommunicator(this);
        communicator.start();
        speaker = gson.fromJson(speakerDB.getGson_object(), SpeakerNew.class);

        titleSession = communicator.getSpeakerSession(speakerDB.getWc_id(), speakerDB.getSpeaker_id());

        initGUI();
    }

    private void initGUI() {

        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(speakerDB.getName());


        // Load the high-resolution "zoomed-in" image.
        zoomImageView = (ImageView) findViewById(
                R.id.backdrop);

        Picasso.with(this).load(speakerDB.getGravatar())
                .placeholder(R.drawable.ic_account_circle_grey600).into(zoomImageView);

        View headerView = LayoutInflater.from(this).inflate(R.layout.item_header_speaker, null);
        info = (TextView) findViewById(R.id.wc_about);
        info.setText(Html.fromHtml(speakerDB.getInfo()));

       /* dp = (ImageView) headerView.findViewById(R.id.speaker_avatar);

        dp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomImageFromThumb(v);
            }
        });
        Picasso.with(this).load(speakerDB.getGravatar())
                .placeholder(R.drawable.ic_account_circle_grey600).into(dp);
*/
        lv = (ListView) findViewById(R.id.session_list_speakers);
        lv.addHeaderView(headerView, null, false);


        if (titleSession != null) {
            final List<String> names = new ArrayList<>(titleSession.keySet());
            lv.setAdapter(new SpeakerDetailAdapter(getApplicationContext(), names));
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                    //As we have 1 header view so the counter starts from 1
                    i--;
                    SessionDB sessionDB = communicator.getSession(speakerDB.getWc_id(), titleSession.get(names.get(i)));
                    Intent intent = new Intent(getApplicationContext(), SessionDetailsActivity.class);
                    intent.putExtra("session", sessionDB);
                    startActivity(intent);
                }
            });
        } else {
            final List<String> names = new ArrayList<>();
            lv.setAdapter(new SpeakerDetailAdapter(getApplicationContext(), names));
            sessionTitle.setVisibility(View.GONE);

        }

        /*zoomImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                AnimatorSet set = new AnimatorSet();

                set.play(ObjectAnimator
                        .ofFloat(container, "x", startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(container,
                                        "y", startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(container,
                                        "scaleX", startScale))
                        .with(ObjectAnimator
                                .ofFloat(container,
                                        "scaleY", startScale));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dp.setAlpha(1f);
                        container.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }


                    @Override
                    public void onAnimationCancel(Animator animation) {
                        dp.setAlpha(1f);
                        container.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;
                visible.set(false);
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wc_speaker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.item_menu_website:
                startWebIntent();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startWebIntent() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(speaker.getLink()));
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(browserIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (communicator == null) {
            communicator = new DBCommunicator(this);
        } else {
            communicator.restart();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (communicator != null)
            communicator.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (communicator != null)
            communicator.close();
    }

    /*private void zoomImageFromThumb(final View thumbView) {

        visible.set(true);

        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        String largeGravatarUrl = speakerDB.getGravatar()
                .substring(0, speakerDB.getGravatar().length() - 6) + "?s=500";
        Picasso.with(this).load(largeGravatarUrl).noPlaceholder().into(zoomImageView);

        container = findViewById(R.id.layout_zoom);
        startBounds = new Rect();
        Rect finalBounds = new Rect();
        Point globalOffset = new Point();

        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.mainLayout)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view.
        thumbView.setAlpha(0f);
        container.setVisibility(View.VISIBLE);

        container.setPivotX(0f);
        container.setPivotY(0f);

        final AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(container, "x",
                startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(container, "y",
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(container, "scaleX",
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(container,
                        "scaleY", startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }


            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
    }*/
}
