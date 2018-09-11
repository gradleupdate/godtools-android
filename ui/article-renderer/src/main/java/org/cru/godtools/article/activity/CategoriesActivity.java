package org.cru.godtools.article.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;

import org.cru.godtools.article.R;
import org.cru.godtools.article.fragment.CategoriesFragment;
import org.cru.godtools.base.tool.activity.BaseSingleToolActivity;

import java.util.Locale;

public class CategoriesActivity extends BaseSingleToolActivity {
    private static final String TAG_MAIN_FRAGMENT = "mainFragment";

    public static void start(@NonNull final Context context, @NonNull final String toolCode,
                             @NonNull final Locale language) {
        final Bundle extras = new Bundle(2);
        populateExtras(extras, toolCode, language);
        final Intent intent = new Intent(context, CategoriesActivity.class).putExtras(extras);
        context.startActivity(intent);
    }

    public CategoriesActivity() {
        super(false);
    }

    // region Lifecycle Events

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_categories);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadInitialFragmentIfNeeded();
    }

    // endregion Lifecycle Events

    @MainThread
    private void loadInitialFragmentIfNeeded() {
        final FragmentManager fm = getSupportFragmentManager();

        // short-circuit if there is a currently attached fragment
        if (fm.findFragmentByTag(TAG_MAIN_FRAGMENT) != null) {
            return;
        }

        // update the displayed fragment
        assert mTool != null;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, CategoriesFragment.newInstance(mTool, mLocale), TAG_MAIN_FRAGMENT)
                .commit();
    }
}