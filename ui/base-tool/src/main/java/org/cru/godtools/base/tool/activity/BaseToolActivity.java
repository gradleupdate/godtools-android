package org.cru.godtools.base.tool.activity;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import org.cru.godtools.base.tool.model.view.ManifestViewUtils;
import org.cru.godtools.base.ui.util.DrawableUtils;
import org.cru.godtools.xml.model.Manifest;

import static org.cru.godtools.base.ui.util.LocaleTypefaceUtils.safeApplyTypefaceSpan;

public abstract class BaseToolActivity extends ImmersiveActivity {
    // App/Action Bar
    @Nullable
    private Menu mToolbarMenu;

    public BaseToolActivity(final boolean immersive) {
        super(immersive);
    }

    // region Lifecycle Events

    @Override
    protected void onSetupActionBar() {
        super.onSetupActionBar();
        updateToolbar();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        mToolbarMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        updateToolbarMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    @CallSuper
    protected void onUpdateToolbar() {}

    @CallSuper
    protected void onUpdateActiveManifest() {
        updateToolbar();
    }

    // endregion Lifecycle Events

    @Nullable
    protected abstract Manifest getActiveManifest();

    // region Toolbar update logic

    private void updateToolbar() {
        final Manifest manifest = getActiveManifest();
        setTitle(safeApplyTypefaceSpan(Manifest.getTitle(manifest), ManifestViewUtils.getTypeface(manifest, this)));

        if (mToolbar != null) {
            // set toolbar background color
            mToolbar.setBackgroundColor(Manifest.getNavBarColor(manifest));

            // set text & controls color
            final int controlColor = Manifest.getNavBarControlColor(manifest);
            mToolbar.setNavigationIcon(DrawableUtils.tint(mToolbar.getNavigationIcon(), controlColor));
            mToolbar.setTitleTextColor(controlColor);
            mToolbar.setSubtitleTextColor(controlColor);
        }

        updateToolbarMenu();
        onUpdateToolbar();
    }

    private void updateToolbarMenu() {
        if (mToolbarMenu != null) {
            // tint all action icons
            final int controlColor = Manifest.getNavBarControlColor(getActiveManifest());
            for (int i = 0; i < mToolbarMenu.size(); ++i) {
                final MenuItem item = mToolbarMenu.getItem(i);
                item.setIcon(DrawableUtils.tint(item.getIcon(), controlColor));
            }
            if (mToolbar != null) {
                mToolbar.setOverflowIcon(DrawableUtils.tint(mToolbar.getOverflowIcon(), controlColor));
            }
        }
    }

    // endregion Toolbar update logic
}
