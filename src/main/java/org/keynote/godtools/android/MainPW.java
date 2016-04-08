package org.keynote.godtools.android;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ccci.gto.android.common.support.v4.app.SimpleLoaderCallbacks;
import org.keynote.godtools.android.broadcast.BroadcastUtil;
import org.keynote.godtools.android.broadcast.Type;
import org.keynote.godtools.android.business.GTLanguage;
import org.keynote.godtools.android.business.GTPackage;
import org.keynote.godtools.android.dao.DBAdapter;
import org.keynote.godtools.android.dao.DBContract.GTLanguageTable;
import org.keynote.godtools.android.everystudent.EveryStudent;
import org.keynote.godtools.android.fragments.PackageListFragment;
import org.keynote.godtools.android.googleAnalytics.EventTracker;
import org.keynote.godtools.android.http.DownloadTask;
import org.keynote.godtools.android.http.GodToolsApiClient;
import org.keynote.godtools.android.http.MetaTask;
import org.keynote.godtools.android.model.HomescreenLayout;
import org.keynote.godtools.android.notifications.GoogleCloudMessagingClient;
import org.keynote.godtools.android.notifications.NotificationsClient;
import org.keynote.godtools.android.service.UpdatePackageListTask;
import org.keynote.godtools.android.snuffy.SnuffyApplication;
import org.keynote.godtools.android.support.v4.content.LivePackagesLoader;

import java.util.ArrayList;
import java.util.List;

import static org.keynote.godtools.android.utils.Constants.APPLICATION_NAME;
import static org.keynote.godtools.android.utils.Constants.ENGLISH_DEFAULT;
import static org.keynote.godtools.android.utils.Constants.EVERY_STUDENT;
import static org.keynote.godtools.android.utils.Constants.FIRST_LAUNCH;
import static org.keynote.godtools.android.utils.Constants.FOUR_LAWS;
import static org.keynote.godtools.android.utils.Constants.KEY_PARALLEL;
import static org.keynote.godtools.android.utils.Constants.KEY_PRIMARY;
import static org.keynote.godtools.android.utils.Constants.KGP;
import static org.keynote.godtools.android.utils.Constants.META;
import static org.keynote.godtools.android.utils.Constants.PREFS_NAME;
import static org.keynote.godtools.android.utils.Constants.SATISFIED;
import static org.keynote.godtools.android.utils.Constants.TRANSLATOR_MODE;
import static org.keynote.godtools.android.utils.Constants.WEB_URL;


public class MainPW extends BaseActionBarActivity implements PackageListFragment.OnPackageSelectedListener,
        DownloadTask.DownloadTaskHandler,
        MetaTask.MetaTaskHandler,
        View.OnClickListener
{
    private static final String TAG = "MainPW";
    private static final int REQUEST_SETTINGS = 1001;

    private static final int REFERENCE_DEVICE_HEIGHT = 960;    // pixels on iPhone w/retina - including title bar
    private static final int REFERENCE_DEVICE_WIDTH = 640;    // pixels on iPhone w/retina - full width

    private static final int LOADER_LIVE_PACKAGES = 1;

    private final PackagesLoaderCallbacks mLoaderCallbacksPackages = new PackagesLoaderCallbacks();

    private int mPageLeft;
    private int mPageTop;
    private int mPageWidth;
    private int mPageHeight;

    @Nullable
    private List<GTPackage> mPackages;
    private String languagePrimary;

    private List<HomescreenLayout> layouts;

    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver broadcastReceiver;

    private SharedPreferences settings;

    /* BEGIN lifecycle */

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);

        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.main_pw);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.titlebar_centered_title);
        TextView titleBar = (TextView) actionBar.getCustomView().findViewById(R.id.titlebar_title);
        titleBar.setText(R.string.app_name);

        setupBroadcastReceiver();
        startLoaders();

        setupLayout();

        if (!isFirstLaunch())
        {
            showLoading();
            GodToolsApiClient.getListOfPackages(META, this);
            settings.edit().putBoolean(TRANSLATOR_MODE, false).apply();
        }

        showLayoutsWithPackages();

        GoogleCloudMessagingClient googleCloudMessagingClient = GoogleCloudMessagingClient.getInstance(getApplicationContext(),
                (SnuffyApplication) getApplication(),
                this,
                settings);

        NotificationsClient notificationsClient = NotificationsClient.getInstance(getApplicationContext(), settings);

        googleCloudMessagingClient.registerForNotificationsIfNecessary(TAG);

        notificationsClient.sendLastUsageUpdateToGodToolsAPI();
        notificationsClient.startTimerForTrackingAppUsageTime();
    }

    void onLoadPackages(@Nullable final List<GTPackage> packages) {
        mPackages = packages;
        showLayoutsWithPackages();

        // XXX: hack to keep languagePrimary in sync with the loaded packages
        languagePrimary = settings.getString(GTLanguage.KEY_PRIMARY, ENGLISH_DEFAULT);
    }

    /* END lifecycle */

    private void startLoaders() {
        final LoaderManager manager = getSupportLoaderManager();
        manager.initLoader(LOADER_LIVE_PACKAGES, null, mLoaderCallbacksPackages);
    }

    private boolean isFirstLaunch()
    {
        boolean isFirst = settings.getBoolean(FIRST_LAUNCH, true);
        if (isFirst)
        {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(FIRST_LAUNCH, false);
            editor.apply();
        }
        return isFirst;
    }

    private void setupLayout()
    {
        layouts = new ArrayList<HomescreenLayout>();

        HomescreenLayout first = new HomescreenLayout();

        first.setLayout((LinearLayout) findViewById(R.id.first_layout));
        first.setTextView((TextView) findViewById(R.id.tv_first));
        first.setImageView((ImageView) findViewById(R.id.iv_first));
        layouts.add(first);

        HomescreenLayout second = new HomescreenLayout();

        second.setLayout((LinearLayout) findViewById(R.id.second_layout));
        second.setTextView((TextView) findViewById(R.id.tv_second));
        second.setImageView((ImageView) findViewById(R.id.iv_second));
        layouts.add(second);

        HomescreenLayout third = new HomescreenLayout();

        third.setLayout((LinearLayout) findViewById(R.id.third_layout));
        third.setTextView((TextView) findViewById(R.id.tv_third));
        third.setImageView((ImageView) findViewById(R.id.iv_third));
        layouts.add(third);

        HomescreenLayout fourth = new HomescreenLayout();

        fourth.setLayout((LinearLayout) findViewById(R.id.fourth_layout));
        fourth.setTextView((TextView) findViewById(R.id.tv_fourth));
        fourth.setImageView((ImageView) findViewById(R.id.iv_fourth));
        layouts.add(fourth);

    }

    private void showLayoutsWithPackages()
    {
        // now there will only be four packages shown on the homescreen
        for (int i = 0; i < 4; i++)
        {
            if (mPackages != null && mPackages.size() > i) {
                GTPackage gtPackage = mPackages.get(i);
                HomescreenLayout layout = layouts.get(i);

                gtPackage.setLayout(layout);

                layout.getLayout().setVisibility(View.VISIBLE);
                layout.getLayout().setClickable(true);
                layout.getLayout().setOnClickListener(this);
                layout.getTextView().setText(gtPackage.getName());

                if (KGP.equals(gtPackage.getCode()))
                    layout.getImageView().setImageResource(R.drawable.gt4_homescreen_kgpicon);
                if (FOUR_LAWS.equals(gtPackage.getCode()))
                    layout.getImageView().setImageResource(R.drawable.gt4_homescreen_4lawsicon);
                if (SATISFIED.equals(gtPackage.getCode()))
                    layout.getImageView().setImageResource(R.drawable.gt4_homescreen_satisfiedicon);
                if (EVERY_STUDENT.equals(gtPackage.getCode()))
                    layout.getImageView().setImageResource(R.drawable.gt4_homescreen_esicon);

            }
            else
            {
                HomescreenLayout layout = layouts.get(i);
                layout.getLayout().setVisibility(View.INVISIBLE);
                layout.getLayout().setClickable(false);
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        removeBroadcastReceiver();
    }

    private void setupBroadcastReceiver()
    {
        broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        broadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (BroadcastUtil.ACTION_STOP.equals(intent.getAction()))
                {
                    Type type = (Type) intent.getSerializableExtra(BroadcastUtil.ACTION_TYPE);

                    if (Type.ENABLE_TRANSLATOR.equals(type))
                    {
                        Toast.makeText(MainPW.this, getString(R.string.translator_enabled), Toast.LENGTH_LONG).show();

                        finish();
                    }
                }
            }
        };

        broadcastManager.registerReceiver(broadcastReceiver, BroadcastUtil.stopFilter());
    }

    private void removeBroadcastReceiver()
    {
        broadcastManager.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.homescreen_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.homescreen_settings:
                onCmd_settings();
                return true;
            case R.id.homescreen_share:
                doCmdShare();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode)
        {
            /* It's possible that both primary and parallel languages that were previously downloaded were changed at the same time.
             * If only one or the other were changed, no harm in running this code, but we do need to make sure the main screen updates
             * if the both were changed.  If if both were changed RESULT_CHANGED_PARALLEL were not added here, then the home screen would
             * not reflect the changed primary language*/
            case RESULT_CHANGED_PRIMARY:
            case RESULT_CHANGED_PARALLEL:
            {
                final String currentLanguage = settings.getString(GTLanguage.KEY_PRIMARY, ENGLISH_DEFAULT);

                EventTracker.track(getApp(), "HomeScreen", currentLanguage);

                break;
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        doSetup();
    }

    private void doSetup()
    {
        EventTracker.track(getApp(), "HomeScreen", settings.getString(GTLanguage.KEY_PRIMARY, ENGLISH_DEFAULT));
        getScreenSize();
    }

    private void getScreenSize()
    {
        /*
         * Although these measurements are not used on this screen, they are passed to and used by
		 * the following screens. At some point maybe all layouts can be updated to relative layout.
		 */
        Rect rect = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);

        rect.top = 0;
        int width;
        int height;
        int left;
        int top;

        double aspectRatioTarget = (double) MainPW.REFERENCE_DEVICE_WIDTH / (double) MainPW.REFERENCE_DEVICE_HEIGHT;
        double aspectRatio = (double) rect.width() / (double) rect.height();

        if (aspectRatio > aspectRatioTarget)
        {
            height = rect.height();
            width = (int) Math.round(height * aspectRatioTarget);
        }
        else
        {
            width = rect.width();
            height = (int) Math.round(width / aspectRatioTarget);
        }

        left = rect.left + (rect.width() - width) / 2;
        top = (rect.height() - height) / 2;

        mPageLeft = left;
        mPageTop = top;
        mPageWidth = width;
        mPageHeight = height;
    }

    private void showLoading()
    {
        supportInvalidateOptionsMenu();

        setSupportProgressBarIndeterminateVisibility(true);
    }

    @Override
    public void onPackageSelected(final GTPackage gtPackage)
    {
        if (gtPackage.getCode().equalsIgnoreCase("everystudent"))
        {
            Intent intent = new Intent(this, EveryStudent.class);
            intent.putExtra("PackageName", gtPackage.getCode());
            addPageFrameToIntent(intent);
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(this, SnuffyPWActivity.class);
        intent.putExtra("PackageName", gtPackage.getCode());
        intent.putExtra("LanguageCode", gtPackage.getLanguage());
        intent.putExtra("ConfigFileName", gtPackage.getConfigFileName());
        intent.putExtra("Status", gtPackage.getStatus());
        addPageFrameToIntent(intent);
        startActivity(intent);
    }

    @Override
    public void onClick(View view)
    {
        if (mPackages != null) {
            for (GTPackage gtPackage : mPackages) {
                Log.i(TAG, view.getId() + " " + gtPackage.getLayout().getLayout().getId());

                if (view.getId() == gtPackage.getLayout().getLayout().getId()) {
                    Log.i(TAG, "clicked: " + gtPackage.getCode());
                    onPackageSelected(gtPackage);
                    break;
                }
            }
        }
    }

    private void hideLoading()
    {
        supportInvalidateOptionsMenu();

        setSupportProgressBarIndeterminateVisibility(false);
    }

    private void addPageFrameToIntent(Intent intent)
    {
        intent.putExtra("PageLeft", mPageLeft);
        intent.putExtra("PageTop", mPageTop);
        intent.putExtra("PageWidth", mPageWidth);
        intent.putExtra("PageHeight", mPageHeight);
    }

    private void onCmd_settings()
    {
        Intent intent = new Intent(this, SettingsPW.class);
        startActivityForResult(intent, REQUEST_SETTINGS);
    }

    private void doCmdShare()
    {
        String messageBody = getString(R.string.share_general_subject);
        messageBody = messageBody.replace(APPLICATION_NAME, getString(R.string.app_name));
        messageBody = messageBody.replace(WEB_URL, "\n"+getString(R.string.app_share_link_base_link));

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        share.putExtra(Intent.EXTRA_TEXT, messageBody);
        startActivity(Intent.createChooser(share, getString(R.string.share_prompt)));
    }

    private SnuffyApplication getApp()
    {
        return (SnuffyApplication) getApplication();
    }

    @Override
    public void metaTaskComplete(List<GTLanguage> languageList, String tag)
    {
        UpdatePackageListTask.run(languageList, DBAdapter.getInstance(this));

        hideLoading();
    }

    @Override
    public void metaTaskFailure(List<GTLanguage> languageList, String tag, int statusCode)
    {
        hideLoading();
    }

    @Override
    public void downloadTaskComplete(String url, String filePath, String langCode, String tag)
    {
        // mark language as downloaded
        final GTLanguage gtl = new GTLanguage();
        gtl.setLanguageCode(langCode);
        gtl.setDownloaded(true);
        DBAdapter.getInstance(this).update(gtl, GTLanguageTable.COL_DOWNLOADED);

        // hide loading state? What does this actually do in MainPW? -DF
        hideLoading();

        // update primary or parallel language
        if (tag.equalsIgnoreCase(KEY_PRIMARY))
        {
            settings.edit().putString(GTLanguage.KEY_PRIMARY, langCode).apply();
        }
        else if (tag.equalsIgnoreCase(KEY_PARALLEL))
        {
            settings.edit().putString(GTLanguage.KEY_PARALLEL, langCode).apply();
        }

        EventTracker.track(getApp(), "HomeScreen", settings.getString(GTLanguage.KEY_PRIMARY, ENGLISH_DEFAULT));
    }

    @Override
    public void downloadTaskFailure(String url, String filePath, String langCode, String tag)
    {
        if (tag.equalsIgnoreCase(KEY_PRIMARY) || tag.equalsIgnoreCase(KEY_PARALLEL))
        {
            Toast.makeText(MainPW.this, getString(R.string.failed_download_resources), Toast.LENGTH_SHORT).show();
        }

        hideLoading();
    }

    private class PackagesLoaderCallbacks extends SimpleLoaderCallbacks<List<GTPackage>> {
        @Override
        public Loader<List<GTPackage>> onCreateLoader(final int id, final Bundle args) {
            switch (id) {
                case LOADER_LIVE_PACKAGES:
                    return new LivePackagesLoader(MainPW.this);
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(@NonNull final Loader<List<GTPackage>> loader,
                                   @Nullable final List<GTPackage> packages) {
            switch (loader.getId()) {
                case LOADER_LIVE_PACKAGES:
                    onLoadPackages(packages);
                    break;
            }
        }
    }
}