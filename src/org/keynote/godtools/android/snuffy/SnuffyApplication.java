package org.keynote.godtools.android.snuffy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.Tracker;

import org.keynote.godtools.android.R;
import org.keynote.godtools.android.business.GTLanguage;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.Vector;

import io.fabric.sdk.android.Fabric;

import static org.keynote.godtools.android.utils.Constants.ENGLISH_DEFAULT;
import static org.keynote.godtools.android.utils.Constants.PREFS_NAME;

public class SnuffyApplication extends Application
{

    // Hold pointers to our created objects for the current SnuffyActivity (if any)
    private Vector<SnuffyPage> snuffyPages;
    public SnuffyPage aboutView;
    public String packageTitle;

    private Locale deviceLocale;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Enable crash reporting
        Fabric.with(this, new Crashlytics());
    }

    public void sendEmailWithContent(Activity callingActivity, String subjectLine, String msgBody)
    {
        try
        {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_SUBJECT, subjectLine);
            intent.putExtra(Intent.EXTRA_TEXT, msgBody);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            callingActivity.startActivity(Intent.createChooser(intent, getApplicationContext().getString(R.string.choose_your_email_provider)));
        } catch (Exception e)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(callingActivity);
            builder.setMessage(R.string.unable_to_send_the_email)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, null);
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public File getDocumentsDir()
    {
        File documentsDir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            documentsDir = getExternalFilesDir(null);
            if (documentsDir != null) {
                Crashlytics.log("documentsDir: " + documentsDir.getPath());
                if (!documentsDir.isDirectory()) {
                    Crashlytics.log("documentsDir doesn't exist");
                    if (!documentsDir.mkdirs()) {
                        Crashlytics.log("unable to create documents directory, falling back to internal directory");
                        documentsDir = null;
                    }
                }
            }
        }
        if (documentsDir == null)
        {
            documentsDir = getFilesDir();
        }
        return documentsDir;
    }

    @NonNull
    public File getResourcesDir() {
        // prefer using external storage when available
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            final File root = getExternalFilesDir(null);
            if (root != null) {
                final File dir = new File(root, "resources");
                Crashlytics.log("Potential External Resources Dir: " + dir);

                // make sure the resources directory exists before returning
                if (dir.isDirectory() || dir.mkdirs()) {
                    return dir;
                }

                // log that we were unable to create external resources directory for any future exception/crash
                Crashlytics.log("unable to create external resources directory");
            }
        }

        // fallback to internal storage
        final File dir = new File(getFilesDir(), "resources");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            // we can't create an internal resources directory, log an error because something crazy may happen!
            Crashlytics.log("unable to create internal resources directory: " + dir);
        }
        return dir;
    }

    private boolean assetExists(String fileName)
    {
        try
        {
            InputStream is = getAssets().open(fileName);
            is.close();
            return true;
        } catch (Exception e)
        {
            return false;
        }
    }

    private boolean fileExists(String fileName)
    {
        File f = new File(getDocumentsDir() + "/" + fileName);
        return f.exists();
    }

    public boolean languageExistsAsAsset(String packageName, String languageCode)
    {
        String testFileName = "Packages/" + packageName + "/" + languageCode + ".xml";
        return assetExists(testFileName);
    }

    public boolean languageExistsAsFile(String packageName, String languageCode)
    {
        String testFileName = "Packages/" + packageName + "/" + languageCode + ".xml";
        return fileExists(testFileName);
    }

    public Tracker getTracker()
    {
        return org.keynote.godtools.android.utils.GoogleAnalytics.getTracker(this);
    }

    public Locale getDeviceLocale()
    {
        return deviceLocale;
    }

    public Vector<SnuffyPage> getSnuffyPages()
    {
        return snuffyPages;
    }

    public void setSnuffyPages(Vector<SnuffyPage> snuffyPages)
    {
        this.snuffyPages = snuffyPages;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        deviceLocale = newConfig.locale;
        super.onConfigurationChanged(newConfig);

        Configuration config = new Configuration();
        config.locale = Locale.getDefault();
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

    }
}
