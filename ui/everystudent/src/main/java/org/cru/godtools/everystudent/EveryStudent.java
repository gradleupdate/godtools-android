package org.cru.godtools.everystudent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.SimpleExpandableListAdapter;

import org.cru.godtools.analytics.AnalyticsService;
import org.cru.godtools.everystudent.analytics.model.EveryStudentAnalyticsScreenEvent;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;

import static org.cru.godtools.everystudent.analytics.model.EveryStudentAnalyticsScreenEvent.SCREEN_EVERYSTUDENT;

@SuppressWarnings("deprecation")
public class EveryStudent extends ExpandableListActivity {
    public static final Locale GA_LANGUAGE_EVERYSTUDENT = Locale.ENGLISH;
    private static final String ROWID = "ROWID";

    private static final int DIALOG_LOADING = 0;

    private static ExpandableListAdapter mAdapter;
    private static ParserThread mParserThread;
    @NonNull
    private AnalyticsService mTracker;

    private List<List<Map<String, String>>> mTopics;
    private List<Map<String, String>> mCategories;

    public static void start(@NonNull final Activity activity) {
        activity.startActivity(new Intent(activity, EveryStudent.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        mTracker = AnalyticsService.getInstance(this);

        EveryStudentPersistance esp = (EveryStudentPersistance) getLastNonConfigurationInstance();

        if (esp != null && esp.getmAdapter() != null && esp.getmTopics() != null && esp.getmCategories() != null) {
            mAdapter = esp.getmAdapter();
            mTopics = esp.getmTopics();
            mCategories = esp.getmCategories();
            setListAdapter(mAdapter);
        } else if (mParserThread != null && mParserThread.isAlive()) {
            mParserThread.setHandler(new ParserHandler());
        } else {
            mParserThread = new ParserThread();
            mParserThread.start();
            showDialog(DIALOG_LOADING);
        }

        Drawable groupIndicator = getResources().getDrawable(R.drawable.expander_group);
        ExpandableListView view = getExpandableListView();
        view.setGroupIndicator(groupIndicator);
        view.setOnChildClickListener(new OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
                                        long id) {
                Intent intent = new Intent(EveryStudent.this, EveryStudentView.class);
                Uri contentUri = Uri.withAppendedPath(EveryStudentProvider.CONTENT_URI, "content");
                Uri contentUriRow =
                        Uri.withAppendedPath(contentUri, mTopics.get(groupPosition).get(childPosition).get(ROWID));
                intent.setData(contentUriRow);
                startActivity(intent);
                return false;
            }
        });

        EventBus.getDefault().post(new EveryStudentAnalyticsScreenEvent(SCREEN_EVERYSTUDENT));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_LOADING:
                ProgressDialog pdlg = new ProgressDialog(this);
                pdlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pdlg.setMessage("Loading. Please wait...");
                pdlg.setIndeterminate(true);
                return pdlg;
        }
        return null;
    }

    public Object onRetainNonConfigurationInstance() {
        return new EveryStudentPersistance(mAdapter, mTopics, mCategories);
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

    @SuppressLint("HandlerLeak")
    private class ParserHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            mAdapter = mParserThread.getAdapter();
            mCategories = mParserThread.getCategories();
            mTopics = mParserThread.getTopics();
            setListAdapter(mAdapter);
            dismissDialog(DIALOG_LOADING);
        }
    }

    class ParserThread extends Thread {
        final List<List<Map<String, String>>> topics = new ArrayList<>();
        final List<Map<String, String>> categories = new ArrayList<>();
        Handler mHandler = new ParserHandler();
        ExpandableListAdapter adapter;

        public void setHandler(Handler h) {
            mHandler = h;
        }

        public ExpandableListAdapter getAdapter() {
            return adapter;
        }

        public List<List<Map<String, String>>> getTopics() {
            return topics;
        }

        public List<Map<String, String>> getCategories() {
            return categories;
        }

        public void run() {
            Uri base = Uri.withAppendedPath(EveryStudentProvider.CONTENT_URI, "base");
            Cursor cur = managedQuery(base, null, null, null, null);

            HashMap<String, List<Map<String, String>>> tempmap = new HashMap<>();
            ArrayList<String> cats = new ArrayList<>();

            if (cur.moveToFirst()) {
                int rowid;
                String category;
                String title;
                do {
                    rowid = cur.getInt(0);
                    category = cur.getString(1);
                    title = cur.getString(2);

                    if (!cats.contains(category)) {
                        cats.add(category);
                    }

                    if (!tempmap.containsKey(category)) {
                        tempmap.put(category, new ArrayList<Map<String, String>>());
                    }
                    HashMap<String, String> map = new HashMap<>();
                    map.put(Constants.NAME, title);
                    map.put(ROWID, String.valueOf(rowid));
                    tempmap.get(category).add(map);
                } while (cur.moveToNext());
            }

            for (String cat : cats) {
                HashMap<String, String> map = new HashMap<>();
                map.put(Constants.NAME, cat);
                categories.add(map);
                topics.add(tempmap.get(cat));
            }

            adapter = new SimpleExpandableListAdapter(EveryStudent.this,
                                                      categories,
                                                      R.layout.simple_expandable_list_item_1,
                                                      new String[]{Constants.NAME}, new int[]{android.R.id.text1},
                                                      topics, R.layout.simple_expandable_list_item_2,
                                                      new String[]{Constants.NAME}, new int[]{android.R.id.text1});

            mHandler.sendEmptyMessage(0);
        }
    }
}
