package org.cru.godtools.tract.adapter;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.ccci.gto.android.common.picasso.view.SimplePicassoImageView;
import org.ccci.gto.android.common.support.v4.adapter.ViewHolderPagerAdapter;
import org.cru.godtools.tract.R;
import org.cru.godtools.tract.R2;
import org.cru.godtools.tract.adapter.ManifestPagerAdapter.PageViewHolder;
import org.cru.godtools.tract.model.CallToAction;
import org.cru.godtools.tract.model.Card;
import org.cru.godtools.tract.model.Header;
import org.cru.godtools.tract.model.Hero;
import org.cru.godtools.tract.model.Manifest;
import org.cru.godtools.tract.model.Page;
import org.cru.godtools.tract.widget.PageContentLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;

public final class ManifestPagerAdapter extends ViewHolderPagerAdapter<PageViewHolder> {
    public interface Callbacks {
        void goToPage(int position);
    }

    @Nullable
    Callbacks mCallbacks;

    @Nullable
    private Manifest mManifest;

    public void setCallbacks(@Nullable final Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void setManifest(@Nullable final Manifest manifest) {
        final Manifest old = mManifest;
        mManifest = manifest;
        if (old != mManifest) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mManifest != null ? mManifest.getPages().size() : 0;
    }

    @NonNull
    @Override
    protected PageViewHolder onCreateViewHolder(@NonNull final ViewGroup parent) {
        return new PageViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.page_manifest_page, parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull final PageViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        assert mManifest != null;
        holder.bind(mManifest.getPages().get(position));
    }

    class PageViewHolder extends ViewHolderPagerAdapter.ViewHolder implements CallToAction.Callbacks {
        @BindView(R2.id.page)
        View mPageView;
        @BindView(R2.id.background_image)
        SimplePicassoImageView mBackgroundImage;

        @BindView(R2.id.page_content_parent)
        NestedScrollView mPageContentParent;
        @BindView(R2.id.page_content_layout)
        PageContentLayout mPageContentLayout;

        // Header & Hero
        @BindView(R2.id.initial_page_content)
        NestedScrollView mHeaderAndHeroLayout;
        @BindView(R2.id.header)
        View mHeader;
        @BindView(R2.id.header_number)
        TextView mHeaderNumber;
        @BindView(R2.id.header_title)
        TextView mHeaderTitle;
        @Nullable
        @BindView(R2.id.hero)
        View mHero;

        // call to action
        @BindView(R2.id.call_to_action)
        View mCallToAction;

        @NonNull
        private final Pools.Pool<Card.CardViewHolder> mRecycledCardViewHolders = new Pools.SimplePool<>(3);
        @NonNull
        private final List<Card.CardViewHolder> mCardViewHolders = new ArrayList<>();

        @BindViews({R2.id.header, R2.id.header_number, R2.id.header_title})
        List<View> mHeaderViews;

        @Nullable
        Page mPage;

        PageViewHolder(@NonNull final View view) {
            super(view);
            ButterKnife.bind(this, view);
            mPageContentParent.setOnScrollChangeListener(mPageContentLayout);
        }

        void bind(@Nullable final Page page) {
            // short-circuit if we aren't changing the page
            if (page == mPage) {
                return;
            }
            mPage = page;

            bindPage(page);
            bindHeader(page);
            Hero.bind(page != null ? page.getHero() : null, mHero);
            bindCards(page);
            CallToAction.bind(page != null ? page.getCallToAction() : null, mCallToAction, this);
        }

        private void bindPage(@Nullable final Page page) {
            mPageView.setBackgroundColor(Page.getBackgroundColor(page));
            Page.bindBackgroundImage(page, mBackgroundImage);
        }

        private void bindHeader(@Nullable final Page page) {
            final Header header = page != null ? page.getHeader() : null;

            ButterKnife.apply(mHeaderViews, (ButterKnife.Action<View>) (view, i) -> view
                    .setVisibility(header != null ? View.VISIBLE : View.GONE));

            if (header != null) {
                mHeader.setBackgroundColor(header.getBackgroundColor());
                header.bindNumber(mHeaderNumber);
                header.bindTitle(mHeaderTitle);
            } else {
                mHeader.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        private void bindCards(@Nullable final Page page) {
            final List<Card> cards = page != null ? page.getCards() : Collections.emptyList();
            final ListIterator<Card.CardViewHolder> i = mCardViewHolders.listIterator();

            // update all visible cards
            for (final Card card : cards) {
                if (i.hasNext()) {
                    i.next().setModel(card);
                } else {
                    // acquire a view holder
                    Card.CardViewHolder holder = mRecycledCardViewHolders.acquire();
                    if (holder == null) {
                        holder = Card.createViewHolder(mPageContentLayout);
                    }

                    // update holder and add it to the layout
                    holder.setModel(card);
                    i.add(holder);
                    mPageContentLayout.addCardView(holder.mRoot);
                }
            }

            // remove any remaining cards that are no longer used
            while (i.hasNext()) {
                final Card.CardViewHolder holder = i.next();
                mPageContentLayout.removeView(holder.mRoot);
                i.remove();
                holder.setModel(null);
                mRecycledCardViewHolders.release(holder);
            }
        }

        @Override
        public void goToNextPage() {
            if (mCallbacks != null && mPage != null) {
                mCallbacks.goToPage(mPage.getPosition() + 1);
            }
        }
    }
}