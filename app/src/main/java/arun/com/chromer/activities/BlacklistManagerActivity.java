package arun.com.chromer.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import arun.com.chromer.R;
import arun.com.chromer.db.BlacklistedApps;
import arun.com.chromer.model.App;
import arun.com.chromer.util.Util;
import arun.com.chromer.views.adapter.BlackListAppRender;
import timber.log.Timber;

public class BlacklistManagerActivity extends AppCompatActivity implements BlackListAppRender.ItemClickListener {

    private List<App> mApps = new ArrayList<>();
    private List<String> sBlacklistedApps = new ArrayList<>();
    private MaterialDialog mProgress;
    private RecyclerView mRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist_manager);
        setupToolbarAndFab();

        mRecyclerView = (RecyclerView) findViewById(R.id.app_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        new AppProcessorTask().execute();
    }

    private void setupToolbarAndFab() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onClick(int position, App app, boolean checked) {
        updateBlacklists(app.getPackageName(), checked);
    }

    private void updateBlacklists(String packageName, boolean checked) {
        if (packageName == null) return;
        List<BlacklistedApps> blacklisted = BlacklistedApps.find(BlacklistedApps.class, "package_name = ?", packageName);
        BlacklistedApps blackListedApp = null;
        if (blacklisted.size() > 0 && blacklisted.get(0).getPackageName().equalsIgnoreCase(packageName)) {
            blackListedApp = blacklisted.get(0);
        }
        if (checked) {
            if (blackListedApp == null) {
                blackListedApp = new BlacklistedApps(packageName);
                blackListedApp.save();
            }
        } else if (blackListedApp != null) blackListedApp.delete();
    }

    private void initList() {
        BlackListAppRender adapter = new BlackListAppRender(BlacklistManagerActivity.this, mApps);
        adapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(adapter);
    }

    private void getBlacklistedPkgsFromDB() {
        List<BlacklistedApps> blacklistedApps = BlacklistedApps.listAll(BlacklistedApps.class);
        for (BlacklistedApps blacklistedApp : blacklistedApps) {
            if (blacklistedApp.getPackageName() != null) {
                sBlacklistedApps.add(blacklistedApp.getPackageName());
            }
        }
        Timber.d(sBlacklistedApps.toString());
    }

    private class AppProcessorTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress = new MaterialDialog.Builder(BlacklistManagerActivity.this)
                    .title(R.string.loading)
                    .content(R.string.please_wait)
                    .progress(true, 0).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            getBlacklistedPkgsFromDB();
            final PackageManager pm = getPackageManager();

            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveList = pm.queryIntentActivities(intent, 0);

            SortedSet<App> sortedSet = new TreeSet<>();
            for (ResolveInfo resolveInfo : resolveList) {
                String pkg = resolveInfo.activityInfo.packageName;
                App app = new App();
                app.setAppName(Util.getAppNameWithPackage(getApplicationContext(), pkg));
                app.setPackageName(pkg);
                app.setBlackListed(sBlacklistedApps.contains(pkg));
                sortedSet.add(app);
            }
            mApps.addAll(sortedSet);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mProgress.dismiss();
            initList();
        }
    }


}