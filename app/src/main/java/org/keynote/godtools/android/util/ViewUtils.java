package org.keynote.godtools.android.util;

import android.support.annotation.Nullable;
import android.widget.TextView;

import org.ccci.gto.android.common.picasso.view.PicassoImageView;
import org.cru.godtools.base.util.FileUtils;
import org.cru.godtools.model.Attachment;
import org.keynote.godtools.android.R;
import org.keynote.godtools.android.model.Tool;

public final class ViewUtils {
    public static void bindShares(@Nullable final TextView view, @Nullable final Tool tool) {
        bindShares(view, tool != null ? tool.getTotalShares() : 0);
    }

    public static void bindShares(@Nullable final TextView view, final int shares) {
        if (view != null) {
            view.setText(view.getResources().getQuantityString(R.plurals.label_tools_shares, shares, shares));
        }
    }

    public static void bindLocalImage(@Nullable final PicassoImageView view, @Nullable final Attachment attachment) {
        bindLocalImage(view, attachment != null && attachment.isDownloaded() ? attachment.getLocalFileName() : null);
    }

    public static void bindLocalImage(@Nullable final PicassoImageView view, @Nullable final String filename) {
        if (view != null) {
            view.setPicassoFile(FileUtils.getFile(view.getContext(), filename));
        }
    }
}