package org.cru.godtools.articles.aem.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;

import org.cru.godtools.article.aem.R;
import org.cru.godtools.articles.aem.fragment.AEMArticleItemFragment;
import org.cru.godtools.base.tool.activity.BaseSingleToolActivity;

import java.util.Locale;

public class AemArticleItemActivity extends BaseSingleToolActivity {
    private static final String TAG_MAIN_FRAGMENT = "mainFragment";
    private static final String EXTRA_ARTICLE_KEY = "extra_article_key";

    private String mArticleKey;

    // region Constructors and Initializers

    public static void start(@NonNull final Context context, @NonNull final String toolCode,
                             @NonNull final Locale language, String articleKey) {
        final Bundle extras = new Bundle();
        populateExtras(extras, toolCode, language);
        extras.putString(EXTRA_ARTICLE_KEY, articleKey);
        final Intent intent = new Intent(context, AemArticleItemActivity.class).putExtras(extras);
        context.startActivity(intent);
    }

    public AemArticleItemActivity() {
        super(false);
    }

    // endregion Constructors and Initializers

    // region Lifecycle Events

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mArticleKey = extras.getString(EXTRA_ARTICLE_KEY, mArticleKey);
        }

        setContentView(R.layout.activity_generic_fragment);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadFragmentIfNeeded();
    }

    // endregion Lifecycle Events

    @MainThread
    private void loadFragmentIfNeeded() {
        final FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentByTag(TAG_MAIN_FRAGMENT) != null) {
            return; // The fragment is already present
        }

        fm.beginTransaction()
                .replace(R.id.frame, AEMArticleItemFragment.newInstance(mTool, mLocale, mArticleKey), TAG_MAIN_FRAGMENT)
                .commit();
    }
}
