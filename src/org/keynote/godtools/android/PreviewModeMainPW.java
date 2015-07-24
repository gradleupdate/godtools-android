package org.keynote.godtools.android;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import org.keynote.godtools.android.business.GTLanguage;
import org.keynote.godtools.android.business.GTPackage;
import org.keynote.godtools.android.dao.DBAdapter;
import org.keynote.godtools.android.everystudent.EveryStudent;
import org.keynote.godtools.android.expandableList.ExpandableListAdapter;
import org.keynote.godtools.android.fragments.AccessCodeDialogFragment;
import org.keynote.godtools.android.googleAnalytics.EventTracker;
import org.keynote.godtools.android.http.DownloadTask;
import org.keynote.godtools.android.http.GodToolsApiClient;
import org.keynote.godtools.android.http.MetaTask;
import org.keynote.godtools.android.service.UpdatePackageListTask;
import org.keynote.godtools.android.snuffy.SnuffyApplication;
import org.keynote.godtools.android.utils.Device;

import java.util.Iterator;
import java.util.List;

import static org.keynote.godtools.android.utils.Constants.AUTH_DRAFT;
import static org.keynote.godtools.android.utils.Constants.FOUR_LAWS;
import static org.keynote.godtools.android.utils.Constants.KGP;
import static org.keynote.godtools.android.utils.Constants.SATISFIED;

public class PreviewModeMainPW extends BaseActionBarActivity implements
        DownloadTask.DownloadTaskHandler,
        MetaTask.MetaTaskHandler,
        View.OnClickListener,
        AccessCodeDialogFragment.AccessCodeDialogListener
{
    private static final String TAG = "PreviewModeMainPW";
    private static final int REQUEST_SETTINGS = 1001;
    private static final String JUST_SWITCHED = "justSwitched";

    public static final int REFERENCE_DEVICE_HEIGHT = 960;    // pixels on iPhone w/retina - including title bar
    public static final int REFERENCE_DEVICE_WIDTH = 640;    // pixels on iPhone w/retina - full width

    private int mPageLeft;
    private int mPageTop;
    private int mPageWidth;
    private int mPageHeight;
    private String languagePrimary;
    private List<GTPackage> packageList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SharedPreferences settings;

    Context context;

    ExpandableListAdapter listAdapter;
    ExpandableListView listView;

    ProgressDialog pdLoading;
    

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_mode_main_pw);
        
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                Log.i(TAG, "Starting refresh");
                refreshDrafts();
            }
        });

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.titlebar_centered_title);
        TextView titleBar = (TextView) actionBar.getCustomView().findViewById(R.id.titlebar_title);
        titleBar.setText(R.string.preview_mode_title);

        context = getApplicationContext();

        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        languagePrimary = settings.getString(GTLanguage.KEY_PRIMARY, "en");

        refreshDrafts();
    }
    
    private void setupExpandableList()
    {
        listView = (ExpandableListView) findViewById(R.id.expandable_list);
        listAdapter = new ExpandableListAdapter(this, packageList, languagePrimary);
        listView.setAdapter(listAdapter);

        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener()
        {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l)
            {
                TextView textView = (TextView) view.findViewById(R.id.tv_trans_view);
                String packageName = (String) textView.getText();
                Log.i(TAG, "Clicked: " + packageName);

                for (GTPackage gtPackage : packageList)
                {
                    if (packageName.equals(gtPackage.getName()))
                    {
                        if (gtPackage.getCode().equalsIgnoreCase("everystudent"))
                        {
                            Intent intent = new Intent(context, EveryStudent.class);
                            intent.putExtra("PackageName", gtPackage.getCode());
                            addPageFrameToIntent(intent);
                            startActivity(intent);
                            return true;
                        }
                        else if (gtPackage.isAvailable())
                        {
                            Intent intent = new Intent(context, SnuffyPWActivity.class);
                            intent.putExtra("PackageName", gtPackage.getCode());
                            intent.putExtra("LanguageCode", gtPackage.getLanguage());
                            intent.putExtra("ConfigFileName", gtPackage.getConfigFileName());
                            intent.putExtra("Status", gtPackage.getStatus());
                            addPageFrameToIntent(intent);
                            startActivity(intent);
                        }
                        else
                        {
                            Toast.makeText(context, getString(R.string.package_not_created), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                return true;
            }
        });
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

            default:
                refreshDrafts();
            case RESULT_CHANGED_PRIMARY:
            case RESULT_CHANGED_PARALLEL:
            {
                getApp().setAppLocale(settings.getString(GTLanguage.KEY_PRIMARY, ""));
                languagePrimary = settings.getString(GTLanguage.KEY_PRIMARY, "");

                getPackageList();

                break;
            }
            case RESULT_PREVIEW_MODE_ENABLED:
            {
                Toast.makeText(PreviewModeMainPW.this, "Translator preview mode is enabled",
                        Toast.LENGTH_LONG).show();

                finish();
                startActivity(getIntent());

                break;
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        SharedPreferences.Editor ed = settings.edit();
        ed.apply();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getPackageList();
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
        int width, height, left, top;

        double aspectRatioTarget = (double) PreviewModeMainPW.REFERENCE_DEVICE_WIDTH / (double) PreviewModeMainPW.REFERENCE_DEVICE_HEIGHT;
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

    private void getPackageList()
    {
        boolean kgpPresent = false;
        boolean satisfiedPresent = false;
        boolean fourlawsPresent = false;
        
        // only return draft packages with translator mode
        List<GTPackage> packageByLanguage = GTPackage.getDraftPackages(PreviewModeMainPW.this, languagePrimary);
        if("en".equals(languagePrimary))
        {
            removeEveryStudent(packageByLanguage);
        }

        Log.i(TAG, "Package size: " + packageByLanguage.size());

        for (GTPackage gtPackage : packageByLanguage)
        {

            if (KGP.equals(gtPackage.getCode())) kgpPresent = true;
            if (SATISFIED.equals(gtPackage.getCode())) satisfiedPresent = true;
            if (FOUR_LAWS.equals(gtPackage.getCode())) fourlawsPresent = true;
        }

        if (!kgpPresent || !satisfiedPresent || !fourlawsPresent)
        {
            
            if (!kgpPresent)
            {
                GTPackage kgpPack = new GTPackage();
                kgpPack.setCode("kgp");
                kgpPack.setName("Knowing God Personally");
                kgpPack.setAvailable(false);
                packageByLanguage.add(kgpPack);
            }

            if (!satisfiedPresent)
            {
                GTPackage satPack = new GTPackage();
                satPack.setCode("satisfied");
                satPack.setName("Satisfied?");
                satPack.setAvailable(false);
                packageByLanguage.add(satPack);
            }

            if (!fourlawsPresent)
            {
                GTPackage fourLawPack = new GTPackage();
                fourLawPack.setCode("fourlaws");
                fourLawPack.setName("The Four Spiritual Laws");
                fourLawPack.setAvailable(false);
                packageByLanguage.add(fourLawPack);
            }
        }

        Log.i(TAG, "Package Size v2: " + packageByLanguage.size());
        
        packageList = packageByLanguage;
        
        setupExpandableList();
    }

    private void removeEveryStudent(List<GTPackage> packages)
    {
        Iterator<GTPackage> i = packages.iterator();
        for(; i.hasNext(); )
        {
            if(i.next().getCode().equals(GTPackage.EVERYSTUDENT_PACKAGE_CODE)) i.remove();
        }
    }

    @Override
    public void onClick(View view)
    {
        Log.i(TAG, "View clicked");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_MENU)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false)
                    .setMessage(R.string.quit_dialog_message)
                    .setPositiveButton(R.string.quit_dialog_confirm, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.quit_dialog_cancel, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            dialogInterface.cancel();
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

    private void refreshDrafts()
    {
        if (Device.isConnected(PreviewModeMainPW.this))
        {
            swipeRefreshLayout.setRefreshing(true);

            GodToolsApiClient.getListOfDrafts(settings.getString(AUTH_DRAFT, ""),
                    languagePrimary, "draft", this);

            GodToolsApiClient.downloadDrafts(getApp(),
                    settings.getString(AUTH_DRAFT, ""),
                    languagePrimary,
                    "draft",
                    this);
        }
        else
        {
            Toast.makeText(PreviewModeMainPW.this, getString(R.string.refresh_no_net),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void doCmdShare()
    {
        String msgBody = getString(R.string.app_share_link);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, msgBody);
        startActivity(Intent.createChooser(share, "Select how you would like to share"));
    }

    private SnuffyApplication getApp()
    {
        return (SnuffyApplication) getApplication();
    }

    private void showAccessCodeDialog()
    {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment frag = (DialogFragment) fm.findFragmentByTag("access_dialog");
        if (frag == null)
        {
            frag = new AccessCodeDialogFragment();
            frag.setCancelable(false);
            frag.show(fm, "access_dialog");
        }
    }

    @Override
    public void onAccessDialogClick(boolean success)
    {
        if (!success)
        {
            if (pdLoading != null) pdLoading.dismiss();
        }
        else
        {
            showLoading("Authenticating access code");
        }
    }

    private void showLoading(String msg)
    {
        pdLoading = new ProgressDialog(PreviewModeMainPW.this);
        pdLoading.setCancelable(false);
        pdLoading.setMessage(msg);
        pdLoading.show();

    }

    @Override
    public void metaTaskComplete(List<GTLanguage> languageList,String tag)
    {
        UpdatePackageListTask.run(languageList, DBAdapter.getInstance(this));
    }

    @Override
    public void metaTaskFailure(List<GTLanguage> languageList, String tag, int statusCode)
    {
        if (401 == statusCode)
        {
            showAccessCodeDialog();
            Toast.makeText(PreviewModeMainPW.this, getString(R.string.expired_passcode), Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(PreviewModeMainPW.this, "Failed to update drafts", Toast.LENGTH_SHORT).show();
        }

        if (tag.equalsIgnoreCase("draft") || tag.equalsIgnoreCase("draft_primary"))
        {
            getPackageList();
        }

        swipeRefreshLayout.setRefreshing(false);
        Log.i(TAG, "Done refreshing");
    }

    @Override
    public void downloadTaskComplete(String url, String filePath, String langCode, String tag)
    {
        if (tag.equalsIgnoreCase("draft"))
        {
            Toast.makeText(PreviewModeMainPW.this, "Drafts have been updated", Toast.LENGTH_SHORT).show();
            getPackageList();

            swipeRefreshLayout.setRefreshing(false);
            Log.i(TAG, "Done refreshing");
        }
        else if (tag.equalsIgnoreCase("draft_primary"))
        {
            languagePrimary = langCode;
            getPackageList();
        }

        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void downloadTaskFailure(String url, String filePath, String langCode, String tag)
    {

        if (tag.equalsIgnoreCase("draft"))
        {
            Toast.makeText(PreviewModeMainPW.this, "Failed to update drafts",
                    Toast.LENGTH_SHORT).show();
        }
        else if (tag.equalsIgnoreCase("draft_primary"))
        {
            getPackageList();
            Toast.makeText(PreviewModeMainPW.this, "Failed to download drafts",
                    Toast.LENGTH_SHORT).show();
        }
        else if (tag.equalsIgnoreCase("primary") || tag.equalsIgnoreCase("parallel"))
        {
            Toast.makeText(PreviewModeMainPW.this, "Failed to download resources",
                    Toast.LENGTH_SHORT).show();
        }

        swipeRefreshLayout.setRefreshing(false);
    }
}