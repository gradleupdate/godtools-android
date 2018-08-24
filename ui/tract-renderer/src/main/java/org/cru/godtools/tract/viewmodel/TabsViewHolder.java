package org.cru.godtools.tract.viewmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayoutUtils;
import android.support.v4.util.Pools;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.annimon.stream.Stream;

import org.ccci.gto.android.common.compat.view.ViewCompat;
import org.cru.godtools.tract.R;
import org.cru.godtools.tract.R2;
import org.cru.godtools.tract.util.ViewUtils;
import org.cru.godtools.xml.model.Base;
import org.cru.godtools.xml.model.Styles;
import org.cru.godtools.xml.model.Tab;
import org.cru.godtools.xml.model.Tabs;
import org.cru.godtools.xml.model.Text;

import butterknife.BindView;

@UiThread
public final class TabsViewHolder extends BaseViewHolder<Tabs> implements TabLayout.OnTabSelectedListener {
    private static final TabViewHolder[] EMPTY_TAB_VIEW_HOLDERS = new TabViewHolder[0];

    @BindView(R2.id.tabs)
    TabLayout mTabs;
    @BindView(R2.id.tab)
    FrameLayout mTabContent;

    private boolean mBinding = false;

    @NonNull
    private TabViewHolder[] mTabContentViewHolders = EMPTY_TAB_VIEW_HOLDERS;
    private final Pools.Pool<TabViewHolder> mRecycledTabViewHolders = new Pools.SimplePool<>(5);

    TabsViewHolder(@NonNull final ViewGroup parent, @Nullable final BaseViewHolder parentViewHolder) {
        super(Tabs.class, parent, R.layout.tract_content_tabs, parentViewHolder);
        setupTabs();
    }

    // region Lifecycle Events

    @UiThread
    @Override
    void onBind() {
        super.onBind();
        bindTabs();
    }

    @Override
    public void onTabSelected(@NonNull final TabLayout.Tab tab) {
        final TabViewHolder holder = showTabContent(tab.getPosition());
        if (!mBinding) {
            holder.trackSelectedAnalyticsEvents();
        }
    }

    @Override
    public void onTabUnselected(final TabLayout.Tab tab) {}

    @Override
    public void onTabReselected(final TabLayout.Tab tab) {}

    // endregion Lifecycle Events

    private void setupTabs() {
        mTabs.addOnTabSelectedListener(this);
        ViewCompat.setClipToOutline(mTabs, true);
    }

    private void bindTabs() {
        mBinding = true;

        // remove all the old tabs
        mTabs.removeAllTabs();
        Stream.of(mTabContentViewHolders)
                .peek(vh -> mTabContent.removeView(vh.mRoot))
                .peek(vh -> vh.bind(null))
                .forEach(mRecycledTabViewHolders::release);
        mTabContentViewHolders = EMPTY_TAB_VIEW_HOLDERS;

        // change the tab styles
        final Styles styles = Base.getStylesParent(mModel);
        final int primaryColor = Styles.getPrimaryColor(styles);
        mTabs.setTabTextColors(primaryColor, Styles.getPrimaryTextColor(styles));

        // update background tint
        ViewUtils.setBackgroundTint(mTabs, primaryColor);

        // add all the current tabs
        if (mModel != null) {
            // create view holders for every tab
            mTabContentViewHolders = Stream.of(mModel.getTabs())
                    .map(this::bindTabContentViewHolder)
                    .toArray(TabViewHolder[]::new);

            // add all the tabs to the TabLayout
            for (final Tab tab : mModel.getTabs()) {
                final Text label = tab.getLabel();
                final TabLayout.Tab tab2 = mTabs.newTab()
                        .setText(Text.getText(label));

                // set the tab background
                TabLayoutUtils.setBackgroundTint(tab2, primaryColor);

                mTabs.addTab(tab2);
            }
        }

        mBinding = false;
    }

    @NonNull
    private TabViewHolder bindTabContentViewHolder(@Nullable final Tab tab) {
        TabViewHolder holder = mRecycledTabViewHolders.acquire();
        if (holder == null) {
            holder = new TabViewHolder(mTabContent, this);
        }
        holder.bind(tab);
        return holder;
    }

    private TabViewHolder showTabContent(final int position) {
        final TabViewHolder holder = mTabContentViewHolders[position];
        if (holder.mRoot.getParent() != mTabContent) {
            mTabContent.removeAllViews();
            mTabContent.addView(holder.mRoot);
        }
        return holder;
    }
}