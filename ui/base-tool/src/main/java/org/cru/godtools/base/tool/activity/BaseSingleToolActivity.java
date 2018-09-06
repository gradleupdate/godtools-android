package org.cru.godtools.base.tool.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;

import org.ccci.gto.android.common.support.v4.app.SimpleLoaderCallbacks;
import org.ccci.gto.android.common.util.os.BundleUtils;
import org.cru.godtools.model.Language;
import org.cru.godtools.model.Tool;
import org.cru.godtools.xml.content.ManifestLoader;
import org.cru.godtools.xml.model.Manifest;

import java.util.Locale;

import static org.cru.godtools.base.Constants.EXTRA_LANGUAGE;
import static org.cru.godtools.base.Constants.EXTRA_TOOL;

public abstract class BaseSingleToolActivity extends BaseToolActivity {
    private static final int LOADER_MANIFEST = 101;

    @Nullable
    protected /*final*/ String mTool = Tool.INVALID_CODE;
    @NonNull
    protected /*final*/ Locale mLocale = Language.INVALID_CODE;

    @Nullable
    protected Manifest mManifest;

    protected static void populateExtras(@NonNull final Bundle extras, @NonNull final String toolCode,
                                         @NonNull final Locale language) {
        extras.putString(EXTRA_TOOL, toolCode);
        BundleUtils.putLocale(extras, EXTRA_LANGUAGE, language);
    }

    public BaseSingleToolActivity(final boolean immersive) {
        super(immersive);
    }

    // region Lifecycle Events

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final Bundle extras = intent != null ? intent.getExtras() : null;
        if (extras != null) {
            mTool = extras.getString(EXTRA_TOOL, mTool);
            mLocale = BundleUtils.getLocale(extras, EXTRA_LANGUAGE, mLocale);
        }

        // finish now if this activity is in an invalid state
        if (!validStartState()) {
            finish();
            return;
        }

        startLoaders();
    }

    // endregion Lifecycle Events

    private boolean validStartState() {
        return mTool != null && !Language.INVALID_CODE.equals(mLocale);
    }

    private void startLoaders() {
        getSupportLoaderManager().initLoader(LOADER_MANIFEST, null, new ManifestLoaderCallbacks());
    }

    void setManifest(@Nullable final Manifest manifest) {
        mManifest = manifest;
        onUpdateActiveManifest();
    }

    @Nullable
    @Override
    protected Manifest getActiveManifest() {
        return mManifest;
    }

    class ManifestLoaderCallbacks extends SimpleLoaderCallbacks<Manifest> {
        @Nullable
        @Override
        public Loader<Manifest> onCreateLoader(final int id, @Nullable final Bundle args) {
            switch (id) {
                case LOADER_MANIFEST:
                    if (mTool != null) {
                        return new ManifestLoader(BaseSingleToolActivity.this, mTool, mLocale);
                    }
                    break;
            }

            return null;
        }

        @Override
        public void onLoadFinished(@NonNull final Loader<Manifest> loader, @Nullable final Manifest manifest) {
            switch (loader.getId()) {
                case LOADER_MANIFEST:
                    setManifest(manifest);
                    break;
            }
        }
    }
}
