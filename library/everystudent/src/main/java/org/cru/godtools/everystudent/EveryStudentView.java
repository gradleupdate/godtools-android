package org.cru.godtools.everystudent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.cru.godtools.analytics.AnalyticsService;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class EveryStudentView extends Activity {
    @NonNull
    private /*final*/ AnalyticsService mAnalytics;

    private String title = "";

    private PowerManager.WakeLock wl = null;

    @Override
    @SuppressLint("ResourceType")
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        mAnalytics = AnalyticsService.getInstance(this);

        SharedPreferences settings = getSharedPreferences(Constants.PREFS_SETTINGS, 0);
        if (settings.getBoolean("wakelock", true)) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
        }

        setContentView(R.layout.everystudent_view);
        this.getWindow().setWindowAnimations(android.R.anim.slide_in_left);

        TextView content = (TextView) this.findViewById(R.id.everystudent_content);

        Uri uri = getIntent().getData();
        Cursor cur = managedQuery(uri, null, null, null, null);

        if (cur.moveToFirst()) {
            title = cur.getString(cur.getColumnIndex(EveryStudentDatabase.TITLE));
            setTitle(title);
            String text = cur.getString(cur.getColumnIndex(EveryStudentDatabase.CONTENT)).replaceFirst("\n", "").trim();
            if (getIntent().getStringExtra(SearchManager.QUERY) != null) {
                content.setText(text, TextView.BufferType.SPANNABLE);
                Spannable str = (Spannable) content.getText();

                String query = getIntent().getStringExtra(SearchManager.QUERY).trim();
                String[] terms = query.split("[\\s]");
                List<String> termsList = Arrays.asList(terms);
                String pattern = "";

                Iterator<String> itr = termsList.iterator();
                while (itr.hasNext()) {
                    pattern += itr.next().trim() + "[^\\s,\\.\\?:;]*";
                    if (itr.hasNext()) {
                        pattern += "|";
                    }
                }

                Pattern myPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                Matcher myMatcher = myPattern.matcher(text);
                while (myMatcher.find()) {
                    str.setSpan(new BackgroundColorSpan(android.graphics.Color.YELLOW), myMatcher.start(),
                                myMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    str.setSpan(new ForegroundColorSpan(android.graphics.Color.BLACK), myMatcher.start(),
                                myMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                content.setText(text);
            }

            recordScreenView();
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.could_not_load_content), Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    private void recordScreenView() {
        mAnalytics.onTrackScreen("everystudent-" + massageTitleToTrainCase());
    }

    private String massageTitleToTrainCase() {
        return title.replaceAll("\\p{Punct}]", "").toLowerCase().replaceAll("\\s", "-");
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAnalytics.onActivityPause(this);
        if (wl != null) {
            wl.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wl != null) {
            wl.acquire();
        }
        mAnalytics.onActivityResume(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.everystudent, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            onSearchRequested();
            return true;
        }
        return false;
    }
}
