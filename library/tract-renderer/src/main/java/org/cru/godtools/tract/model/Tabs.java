package org.cru.godtools.tract.model;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.OnTabSelectedListener;
import android.support.design.widget.TabLayoutUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.Pools;
import android.support.v7.content.res.AppCompatResources;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.annimon.stream.Stream;
import com.google.common.collect.ImmutableList;

import org.ccci.gto.android.common.util.XmlPullParserUtils;
import org.cru.godtools.tract.R;
import org.cru.godtools.tract.R2;
import org.cru.godtools.tract.model.Tab.TabViewHolder;
import org.cru.godtools.tract.util.ViewUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

import static org.cru.godtools.tract.Constants.XMLNS_CONTENT;
import static org.cru.godtools.tract.model.Tab.XML_TAB;

final class Tabs extends Content {
    static final String XML_TABS = "tabs";

    @NonNull
    List<Tab> mTabs = ImmutableList.of();

    private Tabs(@NonNull final Base parent) {
        super(parent);
    }

    @NonNull
    static Tabs fromXml(@NonNull final Base parent, @NonNull final XmlPullParser parser)
            throws IOException, XmlPullParserException {
        return new Tabs(parent).parse(parser);
    }

    @NonNull
    private Tabs parse(@NonNull final XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, XMLNS_CONTENT, XML_TABS);

        // process any child elements
        final List<Tab> tabs = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // process recognized nodes
            switch (parser.getNamespace()) {
                case XMLNS_CONTENT:
                    switch (parser.getName()) {
                        case XML_TAB:
                            tabs.add(Tab.fromXml(this, parser, tabs.size()));
                            continue;
                    }
                    break;
            }

            // skip unrecognized nodes
            XmlPullParserUtils.skipTag(parser);
        }
        mTabs = ImmutableList.copyOf(tabs);

        return this;
    }

    @NonNull
    @Override
    BaseViewHolder createViewHolder(@NonNull final ViewGroup parent, @Nullable final BaseViewHolder parentViewHolder) {
        return new TabsViewHolder(parent, parentViewHolder);
    }

    @UiThread
    public static final class TabsViewHolder extends BaseViewHolder<Tabs> implements OnTabSelectedListener {
        private static final TabViewHolder[] EMPTY_TAB_VIEW_HOLDERS = new TabViewHolder[0];

        @BindView(R2.id.tabs)
        TabLayout mTabs;
        @BindView(R2.id.tab)
        FrameLayout mTabContent;

        @NonNull
        private TabViewHolder[] mTabContentViewHolders = EMPTY_TAB_VIEW_HOLDERS;
        private final Pools.Pool<TabViewHolder> mRecycledTabViewHolders = new Pools.SimplePool<>(5);

        TabsViewHolder(@NonNull final ViewGroup parent, @Nullable final BaseViewHolder parentViewHolder) {
            super(Tabs.class, parent, R.layout.tract_content_tabs, parentViewHolder);
            setupTabs();
        }

        /* BEGIN lifecycle */

        @Override
        void onBind() {
            super.onBind();
            bindTabs();
        }

        @Override
        public void onTabSelected(@NonNull final TabLayout.Tab tab) {
            showTabContent(tab.getPosition());
        }

        @Override
        public void onTabUnselected(final TabLayout.Tab tab) {}

        @Override
        public void onTabReselected(final TabLayout.Tab tab) {}

        /* END lifecycle */

        private void setupTabs() {
            mTabs.addOnTabSelectedListener(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mTabs.setClipToOutline(true);
            }
        }

        private void bindTabs() {
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
                mTabContentViewHolders = Stream.of(mModel.mTabs)
                        .map(this::bindTabContentViewHolder)
                        .toArray(TabViewHolder[]::new);

                // add all the tabs to the TabLayout
                for (final Tab tab : mModel.mTabs) {
                    final Text label = tab.getLabel();
                    final TabLayout.Tab tab2 = mTabs.newTab()
                            .setText(Text.getText(label));

                    // set the tab background
                    Drawable bkg = AppCompatResources.getDrawable(mTabs.getContext(), R.drawable.bkg_tab_label);
                    if (bkg != null) {
                        bkg = DrawableCompat.wrap(bkg).mutate();
                        DrawableCompat.setTint(bkg, primaryColor);
                    }
                    TabLayoutUtils.setBackground(tab2, bkg);

                    mTabs.addTab(tab2);
                }
            }
        }

        @NonNull
        private TabViewHolder bindTabContentViewHolder(@Nullable final Tab tab) {
            TabViewHolder holder = mRecycledTabViewHolders.acquire();
            if (holder == null) {
                holder = Tab.createViewHolder(mTabContent, this);
            }
            holder.bind(tab);
            return holder;
        }

        private void showTabContent(final int position) {
            final TabViewHolder holder = mTabContentViewHolders[position];
            if (holder.mRoot.getParent() != mTabContent) {
                mTabContent.removeAllViews();
                mTabContent.addView(holder.mRoot);
            }
        }
    }
}