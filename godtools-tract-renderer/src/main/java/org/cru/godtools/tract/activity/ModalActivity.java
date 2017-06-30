package org.cru.godtools.tract.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.view.View;

import com.annimon.stream.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.ccci.gto.android.common.util.BundleUtils;
import org.cru.godtools.base.model.Event;
import org.cru.godtools.model.Language;
import org.cru.godtools.tract.R;
import org.cru.godtools.tract.R2;
import org.cru.godtools.tract.content.TractManifestLoader;
import org.cru.godtools.tract.model.Manifest;
import org.cru.godtools.tract.model.Modal;
import org.cru.godtools.tract.model.Modal.ModalViewHolder;
import org.cru.godtools.tract.service.TractManager;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.keynote.godtools.android.model.Tool;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;

import static android.support.v4.app.ActivityOptionsCompat.makeCustomAnimation;
import static org.cru.godtools.base.Constants.EXTRA_LANGUAGE;
import static org.cru.godtools.base.Constants.EXTRA_TOOL;
import static org.cru.godtools.tract.Constants.EXTRA_MANIFEST_FILE_NAME;
import static org.cru.godtools.tract.Constants.EXTRA_MODAL;
import static org.cru.godtools.tract.Constants.EXTRA_PAGE;

public class ModalActivity extends ImmersiveActivity {
    private static final int LOADER_MANIFEST = 101;

    @Nullable
    @BindView(R2.id.modal_root)
    View mModalView;

    @Nullable
    private /*final*/ String mManifestFileName = null;
    /*final*/ long mToolId = Tool.INVALID_ID;
    @NonNull
    /*final*/ Locale mLocale = Language.INVALID_CODE;
    @Nullable
    private /*final*/ String mPageId;
    @Nullable
    private /*final*/ String mModalId;

    @Nullable
    private Modal mModal;
    @Nullable
    private ModalViewHolder mModalViewHolder;

    public static void start(@NonNull final Context context, @NonNull final String manifestFileName, final long toolId,
                             @NonNull final Locale locale, @NonNull final String page, @NonNull final String modal) {
        final Bundle extras = new Bundle(4);
        extras.putString(EXTRA_MANIFEST_FILE_NAME, manifestFileName);
        extras.putLong(EXTRA_TOOL, toolId);
        BundleUtils.putLocale(extras, EXTRA_LANGUAGE, locale);
        extras.putString(EXTRA_PAGE, page);
        extras.putString(EXTRA_MODAL, modal);

        ContextCompat.startActivity(context, new Intent(context, ModalActivity.class).putExtras(extras),
                                    makeCustomAnimation(context, R.anim.activity_fade_in, R.anim.activity_fade_out)
                                            .toBundle());
    }

    /* BEGIN lifecycle */

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modal);

        final Intent intent = getIntent();
        final Bundle extras = intent != null ? intent.getExtras() : null;
        if (extras != null) {
            mManifestFileName = extras.getString(EXTRA_MANIFEST_FILE_NAME, mManifestFileName);
            mToolId = extras.getLong(EXTRA_TOOL, mToolId);
            mLocale = BundleUtils.getLocale(extras, EXTRA_LANGUAGE, mLocale);
            mPageId = extras.getString(EXTRA_PAGE, mPageId);
            mModalId = extras.getString(EXTRA_MODAL, mModalId);
        }

        checkForAlreadyLoadedManifest();
        startLoaders();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        setupModalViewHolder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @MainThread
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onContentEvent(@NonNull final Event event) {
        checkForDismissEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    /* END lifecycle */

    private void checkForAlreadyLoadedManifest() {
        if (mManifestFileName != null) {
            final ListenableFuture<Manifest> manifest =
                    TractManager.getInstance(this).getManifest(mManifestFileName, mToolId, mLocale);
            if (manifest.isDone()) {
                try {
                    updateModal(Futures.getDone(manifest));
                } catch (final ExecutionException e) {
                    updateModal(null);
                }
            }
        }
    }

    private void startLoaders() {
        getSupportLoaderManager().initLoader(LOADER_MANIFEST, null, new ManifestLoaderCallbacks());
    }

    void updateModal(@Nullable final Manifest manifest) {
        mModal = Optional.ofNullable(manifest)
                .map(m -> m.findPage(mPageId))
                .map(p -> p.findModal(mModalId))
                .orElse(null);
        if (mModal == null) {
            finish();
        }
        updateModalViewHolder();
    }

    private void setupModalViewHolder() {
        if (mModalView != null) {
            mModalViewHolder = Modal.getViewHolder(mModalView);
            updateModalViewHolder();
        }
    }

    private void updateModalViewHolder() {
        if (mModalViewHolder != null) {
            mModalViewHolder.bind(mModal);
        }
    }

    private void checkForDismissEvent(@NonNull final Event event) {
        if (mModal != null) {
            if (mModal.getDismissListeners().contains(event.id)) {
                finish();
            }
        }
    }

    class ManifestLoaderCallbacks implements LoaderManager.LoaderCallbacks<Manifest> {
        @Nullable
        @Override
        public Loader<Manifest> onCreateLoader(final int id, @Nullable final Bundle args) {
            switch (id) {
                case LOADER_MANIFEST:
                    return new TractManifestLoader(ModalActivity.this, mToolId, mLocale);
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(@NonNull final Loader<Manifest> loader, @Nullable final Manifest manifest) {
            switch (loader.getId()) {
                case LOADER_MANIFEST:
                    updateModal(manifest);
                    break;
            }
        }

        @Override
        public void onLoaderReset(final Loader<Manifest> loader) {
            switch (loader.getId()) {
                case LOADER_MANIFEST:
                    // no-op
                    break;
            }
        }
    }
}
