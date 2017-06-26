package org.keynote.godtools.android.adapter;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ccci.gto.android.common.db.util.CursorUtils;
import org.ccci.gto.android.common.picasso.view.PicassoImageView;
import org.ccci.gto.android.common.recyclerview.adapter.CursorAdapter;
import org.keynote.godtools.android.R;
import org.keynote.godtools.android.db.Contract.ToolTable;
import org.keynote.godtools.android.model.Tool;
import org.keynote.godtools.android.util.ViewUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

import static org.keynote.godtools.android.util.ViewUtils.bindShares;

public class ToolsAdapter extends CursorAdapter<ToolsAdapter.ToolViewHolder> {
    public static final String COL_BANNER = "banner";
    public interface Callbacks {
        void onToolInfo(long id);

        void onToolSelect(long id);

        void onToolAdd(long id);
    }

    final boolean mHideAddAction;

    @Nullable
    Callbacks mCallbacks;

    public ToolsAdapter(final boolean hideAddAction) {
        mHideAddAction = hideAddAction;
    }

    public void setCallbacks(@Nullable final Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    public ToolViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ToolViewHolder(LayoutInflater.from(parent.getContext())
                                              .inflate(R.layout.list_item_resource_card, parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull final ToolViewHolder holder, @Nullable final Cursor cursor,
                                    final int position) {
        holder.bind(cursor);
    }

    class ToolViewHolder extends BaseViewHolder {
        @Nullable
        @BindView(R.id.banner)
        PicassoImageView mBanner;
        @Nullable
        @BindView(R.id.title)
        TextView mTitleView;
        @Nullable
        @BindView(R.id.shares)
        TextView mSharesView;
        @Nullable
        @BindView(R.id.download_progress)
        ProgressBar mDownloadProgress;
        @Nullable
        @BindView(R.id.action_add)
        View mActionAdd;
        @Nullable
        @BindViews({R.id.action_add, R.id.divider_download})
        List<View> mAddViews;

        long mId;
        @Nullable
        String mTitle;
        @Nullable
        String mBannerFile;
        int mShares = 0;
        boolean mAdded = false;
        boolean mDownloading = false;
        boolean mDownloaded = true;

        ToolViewHolder(@NonNull final View view) {
            super(view);
            if (mAddViews != null) {
                ButterKnife.apply(mAddViews, (ButterKnife.Action<View>) (v, i) -> v
                        .setVisibility(mHideAddAction ? View.GONE : View.VISIBLE));
            }
        }

        void bind(@Nullable final Cursor cursor) {
            // update data from Cursor
            if (cursor != null) {
                mId = CursorUtils.getLong(cursor, ToolTable.COLUMN_ID, Tool.INVALID_ID);
                mTitle = CursorUtils.getString(cursor, ToolTable.COLUMN_NAME, null);
                mBannerFile = CursorUtils.getString(cursor, COL_BANNER, null);
                mAdded = CursorUtils.getBool(cursor, ToolTable.COLUMN_ADDED, false);
                mShares = CursorUtils.getInt(cursor, ToolTable.COLUMN_SHARES, 0);
            } else {
                mId = Tool.INVALID_ID;
                mTitle = null;
                mBannerFile = null;
                mShares = 0;
                mAdded = false;
                mDownloaded = false;
                mDownloading = false;
            }

            // update any bound views
            ViewUtils.bindLocalImage(mBanner, mBannerFile);
            if (mTitleView != null) {
                mTitleView.setText(mTitle);
            }
            bindShares(mSharesView, mShares);
            if (mActionAdd != null) {
                mActionAdd.setEnabled(!mAdded);
            }
            if (mDownloadProgress != null) {
                mDownloadProgress.setVisibility(mAdded && (mDownloading || !mDownloaded) ? View.VISIBLE : View.GONE);
            }
        }

        @Optional
        @OnClick(R.id.root)
        void select() {
            if (mCallbacks != null) {
                mCallbacks.onToolSelect(mId);
            }
        }

        @Optional
        @OnClick(R.id.action_add)
        void add() {
            if (mCallbacks != null) {
                mCallbacks.onToolAdd(mId);
            }
        }

        @Optional
        @OnClick(R.id.action_info)
        void info() {
            if (mCallbacks != null) {
                mCallbacks.onToolInfo(mId);
            }
        }
    }
}