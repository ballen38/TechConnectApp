package org.techconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.centum.techconnect.R;
import org.techconnect.model.FlowChart;
import org.techconnect.resources.ResourceHandler;
import org.techconnect.services.TCService;
import org.techconnect.sql.TCDatabaseHelper;
import org.techconnect.views.CommentThreadView;
import org.techconnect.views.ResourcesView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GuideActivity extends AppCompatActivity {

    public static String EXTRA_CHART = "org.techconnect.guideactivity.flowchart";

    @Bind(R.id.coordinatorLayout)
    CoordinatorLayout coordinatorLayout;
    @Bind(R.id.download_fab)
    FloatingActionButton fab;
    @Bind(R.id.header_imageView)
    ImageView headerImageView;
    @Bind(R.id.nestedScrollView)
    NestedScrollView nestedScrollView;
    @Bind(R.id.type_textView)
    TextView typeTextView;
    @Bind(R.id.score_textView)
    TextView scoreTextView;
    @Bind(R.id.version_textView)
    TextView versionTextView;
    @Bind(R.id.description_textView)
    TextView descriptionTextView;
    @Bind(R.id.update_TextView)
    TextView updateTextView;

    @Bind(R.id.tab_layout)
    TabLayout tabLayout;
    @Bind(R.id.tabContentContainer)
    FrameLayout tabContentContainer;

    private CommentThreadView commentThreadView;
    private ResourcesView resourcesView;

    private FlowChart flowChart;
    private boolean inDB = true;
    private boolean downloadingChart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        commentThreadView = (CommentThreadView) getLayoutInflater().inflate(R.layout.comment_thread_view, tabContentContainer, false);
        resourcesView = (ResourcesView) getLayoutInflater().inflate(R.layout.resources_view, tabContentContainer, false);

        tabContentContainer.addView(commentThreadView);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabContentContainer.removeAllViews();
                if (tab.getPosition() == 0) {
                    tabContentContainer.addView(commentThreadView);
                } else {
                    tabContentContainer.addView(resourcesView);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        if (getIntent() != null && getIntent().hasExtra(EXTRA_CHART)) {
            flowChart = getIntent().getParcelableExtra(EXTRA_CHART);
            checkDBForFlowchart();
        }
    }

    private void checkDBForFlowchart() {
        inDB = false;
        if (flowChart != null) {
            if (TCDatabaseHelper.get(this).hasChart(flowChart.getId())) {
                inDB = true;
            }
        }
        updateViews();
    }

    @OnClick(R.id.download_fab)
    protected void onFabAction() {
        if (!downloadingChart) {
            if (inDB) {
                onPlay();
            } else {
                reloadChart();
            }
        }
    }

    @OnClick(R.id.update_TextView)
    protected void onUpdate() {
        if (!downloadingChart && inDB) {
            reloadChart();
        }
    }

    private void updateViews() {
        if (flowChart != null) {
            setTitle(flowChart.getName());
        }
        if (inDB) {
            fab.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            updateTextView.setVisibility(View.VISIBLE);
        } else {
            fab.setImageResource(R.drawable.ic_file_download_white_48dp);
            updateTextView.setVisibility(View.GONE);
        }
        typeTextView.setText(flowChart.getType().toString().toLowerCase());
        scoreTextView.setText(flowChart.getScore() + "");
        versionTextView.setText(flowChart.getVersion());
        descriptionTextView.setText(flowChart.getDescription());
        resourcesView.setResources(flowChart.getResources());
        commentThreadView.setComments(flowChart.getComments());
        updateHeaderImage();
    }

    private void updateHeaderImage() {
        if (flowChart.getImage() != null && !TextUtils.isEmpty(flowChart.getImage())) {
            if (ResourceHandler.get(this).hasStringResource(flowChart.getImage())) {
                // Load offline image
                Picasso.with(this)
                        .load(getFileStreamPath(
                                ResourceHandler.get().getStringResource(flowChart.getImage())))
                        .into(headerImageView);
            } else {
                // Load online image
                Picasso.with(this)
                        .load(flowChart.getImage())
                        .into(headerImageView);
            }
        }
    }

    private void reloadChart() {
        if (flowChart != null) {
            downloadingChart = true;
            fab.setImageResource(R.drawable.ic_sync_white_48dp);
            fab.setEnabled(false);
            updateTextView.setEnabled(false);
            TCService.startLoadCharts(this, new String[]{flowChart.getId()}, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);
                    downloadingChart = false;
                    fab.setEnabled(true);
                    updateTextView.setEnabled(true);
                    checkDBForFlowchart();
                    if (resultCode == TCService.LOAD_CHARTS_RESULT_SUCCESS) {
                        Snackbar.make(coordinatorLayout, getString(R.string.guide_updated), Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(coordinatorLayout, getString(R.string.fail_update_guide), Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void onPlay() {
        if (flowChart != null) {
            Intent intent = new Intent(this, PlayGuideActivity.class);
            intent.putExtra(PlayGuideActivity.EXTRA_CHART_ID, flowChart.getId());
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.header_imageView)
    protected void onHeaderImageAction() {
        Intent intent = new Intent(this, ImageViewActivity.class);
        if (ResourceHandler.get().hasStringResource(flowChart.getImage())) {
            intent.putExtra(ImageViewActivity.EXTRA_PATH,
                    ResourceHandler.get(this).getStringResource(flowChart.getImage()));
        } else {
            intent.putExtra(ImageViewActivity.EXTRA_URL, flowChart.getImage());
        }
        startActivity(intent);
    }
}
