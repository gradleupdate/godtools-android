package org.keynote.godtools.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.keynote.godtools.android.business.GTLanguage;
import org.keynote.godtools.android.business.GTPackage;
import org.keynote.godtools.android.business.GTPackageReader;
import org.keynote.godtools.android.dao.DBAdapter;
import org.keynote.godtools.android.fragments.AlertDialogFragment;
import org.keynote.godtools.android.fragments.LanguageDialogFragment;
import org.keynote.godtools.android.fragments.LanguageDialogFragment.OnLanguageChangedListener;
import org.keynote.godtools.android.fragments.PackageListFragment;
import org.keynote.godtools.android.fragments.PackageListFragment.OnPackageSelectedListener;
import org.keynote.godtools.android.http.DownloadTask;
import org.keynote.godtools.android.http.GodToolsApiClient;
import org.keynote.godtools.android.http.HttpTask;
import org.keynote.godtools.android.snuffy.SnuffyApplication;
import org.keynote.godtools.android.utils.Device;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class MainPW extends ActionBarActivity implements OnLanguageChangedListener, OnPackageSelectedListener, DownloadTask.DownloadTaskHandler, HttpTask.HttpTaskHandler {
    private static final String PREFS_NAME = "GodTools";
    private static final String TAG_LIST = "PackageList";
    private static final String TAG_DIALOG_LANGUAGE = "LanguageDialog";

    private static final int REQUEST_SETTINGS = 1001;
    public static final int RESULT_DOWNLOAD_PRIMARY = 2001;
    public static final int RESULT_DOWNLOAD_PARALLEL = 2002;
    public static final int RESULT_CHANGED_PRIMARY = 2003;

    public static final int REFERENCE_DEVICE_HEIGHT = 960;    // pixels on iPhone w/retina - including title bar
    public static final int REFERENCE_DEVICE_WIDTH = 640;    // pixels on iPhone w/retina - full width
    private int mPageLeft;
    private int mPageTop;
    private int mPageWidth;
    private int mPageHeight;

    private String languagePrimary;
    private String languagePhone;

    private List<GTPackage> packageList;
    private GTLanguage gtLanguage;

    PackageListFragment packageFrag;
    View vLoading;
    ImageButton ibRefresh;
    TextView tvTask;


    boolean isDownloading;
    String authorization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_pw);
        setUpActionBar();

        vLoading = findViewById(R.id.contLoading);
        tvTask = (TextView) findViewById(R.id.tvTask);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        languagePrimary = settings.getString(GTLanguage.KEY_PRIMARY, "en");
        languagePhone = ((SnuffyApplication) getApplication()).getDeviceLocale().getLanguage();
        authorization = getString(R.string.key_authorization_generic);


        packageList = getPackageList(); // get the packages for the primary language

        FragmentManager fm = getSupportFragmentManager();
        packageFrag = (PackageListFragment) fm.findFragmentByTag(TAG_LIST);
        if (packageFrag == null) {
            packageFrag = PackageListFragment.newInstance(packageList);
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.contList, packageFrag, TAG_LIST);
            ft.commit();
        }

        computeDimension();

        if (shouldUpdateLanguageSettings()) {
            showLanguageDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {
            case RESULT_CHANGED_PRIMARY: {
                languagePrimary = data.getStringExtra("primaryCode");
                packageList = getPackageList();
                packageFrag.refreshList(packageList);

                SnuffyApplication app = (SnuffyApplication) getApplication();
                app.setAppLocale(data.getStringExtra("primaryCode"));

                break;
            }
            case RESULT_DOWNLOAD_PRIMARY: {
                // start the download
                String code = data.getStringExtra("primaryCode");
                gtLanguage = GTLanguage.getLanguage(MainPW.this, code);

                showLoading("Downloading resources...");
                GodToolsApiClient.downloadLanguagePack((SnuffyApplication) getApplication(),
                        gtLanguage.getLanguageCode(),
                        "primary",
                        authorization,
                        this);

                break;
            }
            case RESULT_DOWNLOAD_PARALLEL: {

                // refresh the list if the primary language was changed
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String primaryCode = settings.getString(GTLanguage.KEY_PRIMARY, "en");
                if (!languagePrimary.equalsIgnoreCase(primaryCode)) {
                    languagePrimary = primaryCode;
                    packageList = getPackageList();
                    packageFrag.refreshList(packageList);
                }

                String code = data.getStringExtra("parallelCode");
                gtLanguage = GTLanguage.getLanguage(MainPW.this, code);
                showLoading("Downloading resources...");
                GodToolsApiClient.downloadLanguagePack((SnuffyApplication) getApplication(),
                        code,
                        "parallel",
                        authorization,
                        this);
                break;
            }
            case 1234: {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayShowCustomEnabled(true);
                ibRefresh = (ImageButton) findViewById(R.id.ibRefresh);

                // refresh the list
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String primaryCode = settings.getString(GTLanguage.KEY_PRIMARY, "en");

                if (!languagePrimary.equalsIgnoreCase(primaryCode)) {
                    SnuffyApplication app = (SnuffyApplication) getApplication();
                    app.setAppLocale(primaryCode);
                }


                languagePrimary = primaryCode;
                packageList = getPackageList();
                packageFrag.refreshList(packageList);


                Toast.makeText(MainPW.this, "Translator mode is enabled", Toast.LENGTH_LONG).show();
                break;
            }
            case 2345: {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayShowCustomEnabled(false);
                ibRefresh = null;

                // refresh the list
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String primaryCode = settings.getString(GTLanguage.KEY_PRIMARY, "en");

                if (!languagePrimary.equalsIgnoreCase(primaryCode)) {
                    SnuffyApplication app = (SnuffyApplication) getApplication();
                    app.setAppLocale(primaryCode);
                }

                languagePrimary = primaryCode;
                packageList = getPackageList();
                packageFrag.refreshList(packageList);

                Toast.makeText(MainPW.this, "Translator mode is disabled", Toast.LENGTH_LONG).show();
                break;
            }
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_settings);
        item.setEnabled(!isDownloading);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsPW.class);
                startActivityForResult(intent, REQUEST_SETTINGS);
                break;
        }

        return true;
    }

    private boolean isTranslatorModeEnabled() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return settings.getBoolean("TranslatorMode", false);
    }

    public void refresh(View view) {

        if (Device.isConnected(MainPW.this)) {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String authorization = settings.getString("authorization", getString(R.string.key_authorization_generic));
            showLoading("Downloading drafts...");
            GodToolsApiClient.getListOfDrafts(authorization, languagePrimary, "draft", this);
        } else {
            Toast.makeText(MainPW.this, "Internet connection is required", Toast.LENGTH_SHORT).show();
        }
    }


    private void setUpActionBar() {

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(R.layout.custom_actionbar);

        if (isTranslatorModeEnabled()) {
            actionBar.setDisplayShowCustomEnabled(true);
        }

    }

    private List<GTPackage> getPackageList() {
        if (isTranslatorModeEnabled()) {
            return GTPackage.getPackageByLanguage(MainPW.this, languagePrimary);
        } else {
            return GTPackage.getLivePackages(MainPW.this, languagePrimary);
        }
    }


    private void recreateActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            recreate();
        } else {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    private void showLanguageDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment frag = (DialogFragment) fm.findFragmentByTag(TAG_DIALOG_LANGUAGE);
        if (frag == null) {
            Locale locale = new Locale(languagePhone);
            frag = LanguageDialogFragment.newInstance(locale.getDisplayName(), locale.getLanguage());
            frag.show(fm, TAG_DIALOG_LANGUAGE);
        }
    }

    private boolean shouldUpdateLanguageSettings() {

        // check first if the we support the phones language
        gtLanguage = GTLanguage.getLanguage(this, languagePhone);
        if (gtLanguage == null)
            return false;

        return !languagePrimary.equalsIgnoreCase(languagePhone);
    }

    private void computeDimension() {
        Rect r = new Rect();
        Window w = getWindow();
        w.getDecorView().getWindowVisibleDisplayFrame(r);

        mPageLeft = 0;
        mPageTop = 0;
        mPageWidth = r.width();
        mPageHeight = r.height();
    }

    private void addPageFrameToIntent(Intent intent) {
        intent.putExtra("PageLeft", mPageLeft);
        intent.putExtra("PageTop", mPageTop);
        intent.putExtra("PageWidth", mPageWidth);
        intent.putExtra("PageHeight", mPageHeight);
    }

    private void showLoading(String msg) {
        isDownloading = true;
        supportInvalidateOptionsMenu();
        tvTask.setText(msg);
        vLoading.setVisibility(View.VISIBLE);
        packageFrag.disable();

        if (ibRefresh != null) {
            ibRefresh.setEnabled(false);
        }
    }

    private void hideLoading() {
        isDownloading = false;
        supportInvalidateOptionsMenu();
        tvTask.setText("");
        vLoading.setVisibility(View.GONE);
        packageFrag.enable();

        if (ibRefresh != null) {
            ibRefresh.setEnabled(true);
        }
    }

    @Override
    public void onLanguageChanged(String name, String code) {

        gtLanguage = GTLanguage.getLanguage(MainPW.this, code);
        if (gtLanguage.isDownloaded()) {
            languagePrimary = gtLanguage.getLanguageCode();
            packageList = getPackageList();
            packageFrag.refreshList(packageList);

            SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(GTLanguage.KEY_PRIMARY, code);

            String parallelLanguage = settings.getString(GTLanguage.KEY_PARALLEL, "");
            if (code.equalsIgnoreCase(parallelLanguage))
                editor.putString(GTLanguage.KEY_PARALLEL, "");

            editor.commit();

            SnuffyApplication app = (SnuffyApplication) getApplication();
            app.setAppLocale(code);


        } else {

            if (Device.isConnected(MainPW.this)) {
                showLoading("Downloading resources...");
                GodToolsApiClient.downloadLanguagePack((SnuffyApplication) getApplication(),
                        gtLanguage.getLanguageCode(),
                        "primary",
                        authorization,
                        this);
            } else {
                // TODO: show dialog, Internet connection is required to download the resources
                Toast.makeText(this, "Unable to download resources. Internet connection unavailable.", Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    public void onPackageSelected(GTPackage gtPackage) {

        Intent intent = new Intent(this, SnuffyPWActivity.class);
        intent.putExtra("PackageName", gtPackage.getCode());
        intent.putExtra("LanguageCode", gtPackage.getLanguage());
        intent.putExtra("ConfigFileName", gtPackage.getConfigFileName());
        intent.putExtra("Status", gtPackage.getStatus());
        addPageFrameToIntent(intent);
        startActivity(intent);

    }

    @Override
    public void httpTaskComplete(String url, InputStream is, int statusCode, String tag) {
        if (tag.equalsIgnoreCase("draft")) {
            // process the input stream
            new UpdatePackageListTask().execute(is);

        }
    }

    @Override
    public void downloadTaskComplete(String url, String filePath, String tag) {

        if (tag.equalsIgnoreCase("primary")) {
            // set the language code as default
            languagePrimary = gtLanguage.getLanguageCode();

            SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(GTLanguage.KEY_PRIMARY, gtLanguage.getLanguageCode());
            editor.commit();

            packageList = getPackageList();
            packageFrag.refreshList(packageList);

            SnuffyApplication app = (SnuffyApplication) getApplication();
            app.setAppLocale(languagePrimary);

            // update the database
            gtLanguage.setDownloaded(true);
            gtLanguage.update(MainPW.this);

        } else if (tag.equalsIgnoreCase("parallel")) {

            SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(GTLanguage.KEY_PARALLEL, gtLanguage.getLanguageCode());
            editor.commit();

            // update the database
            gtLanguage.setDownloaded(true);
            gtLanguage.update(MainPW.this);

        } else if (tag.equalsIgnoreCase("draft")) {
            packageList = getPackageList();
            packageFrag.refreshList(packageList);
        }


        hideLoading();
    }

    @Override
    public void downloadTaskFailure(String url, String filePath, String tag) {
        // TODO: show dialog to inform the user that the download failed
        Toast.makeText(MainPW.this, "Failed to download resources", Toast.LENGTH_SHORT).show();
        // if drafts failed delete all drafts
        hideLoading();
    }

    @Override
    public void httpTaskFailure(String url, InputStream is, int statusCode, String tag) {
        hideLoading();
        Toast.makeText(MainPW.this, "failed", Toast.LENGTH_SHORT).show();
    }

    private class UpdatePackageListTask extends AsyncTask<InputStream, Void, Boolean> {
        DBAdapter mAdapter;
        boolean mNewDraftsAvailable;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mAdapter = DBAdapter.getInstance(MainPW.this);
            mNewDraftsAvailable = false;
        }

        @Override
        protected Boolean doInBackground(InputStream... params) {

            InputStream is = params[0];
            List<GTLanguage> languageList = GTPackageReader.processMetaResponse(is);

            GTLanguage language = languageList.get(0);
            List<GTPackage> packagesDraft = language.getPackages();

            if (packagesDraft.size() == 0) {
                return false;
            }

            for (GTPackage gtp : packagesDraft) {

                GTPackage dbPackage = mAdapter.getGTPackage(gtp.getCode(), gtp.getLanguage(), gtp.getStatus());
                if (dbPackage == null) {
                    mAdapter.insertGTPackage(gtp);
                    mNewDraftsAvailable = true;
                } else if (gtp.getVersion() > dbPackage.getVersion()) {
                    mAdapter.updateGTPackage(gtp);
                    mNewDraftsAvailable = true;
                }
            }

            return mNewDraftsAvailable;
        }

        @Override
        protected void onPostExecute(Boolean shouldDownload) {
            super.onPostExecute(shouldDownload);

            if (shouldDownload) {

                SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String authorization = settings.getString("authorization", getString(R.string.key_authorization_generic));
                GodToolsApiClient.downloadDrafts((SnuffyApplication) getApplication(), authorization, languagePrimary, "draft", MainPW.this);


            } else {

                FragmentManager fm = getSupportFragmentManager();
                DialogFragment frag = (DialogFragment) fm.findFragmentByTag("alert_dialog");
                if (frag == null) {
                    frag = AlertDialogFragment.newInstance("Drafts", "No updates available");
                    frag.setCancelable(false);
                    frag.show(fm, "alert_dialog");
                }

                hideLoading();
            }
        }
    }
}
