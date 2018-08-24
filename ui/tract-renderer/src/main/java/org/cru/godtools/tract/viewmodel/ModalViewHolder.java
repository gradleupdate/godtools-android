package org.cru.godtools.tract.viewmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import org.cru.godtools.tract.R;
import org.cru.godtools.tract.R2;
import org.cru.godtools.xml.model.Modal;

import butterknife.BindView;

public class ModalViewHolder extends ParentViewHolder<Modal> {
    @Nullable
    @BindView(R2.id.title)
    TextView mTitle;

    ModalViewHolder(@NonNull final View root) {
        super(Modal.class, root, null);
    }

    @NonNull
    public static ModalViewHolder forView(@NonNull final View root) {
        final ModalViewHolder holder = forView(root, ModalViewHolder.class);
        return holder != null ? holder : new ModalViewHolder(root);
    }

    @Override
    void onBind() {
        super.onBind();
        TextViewUtils.bind(mModel != null ? mModel.getTitle() : null, mTitle, R.dimen.text_size_modal_title, null);
    }
}