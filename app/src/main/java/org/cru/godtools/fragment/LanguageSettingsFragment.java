package org.cru.godtools.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cru.godtools.R;
import org.cru.godtools.activity.LanguageSelectionActivity;
import org.cru.godtools.base.util.LocaleUtils;

import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;

public class LanguageSettingsFragment extends BasePlatformFragment {
    @Nullable
    @BindView(R.id.primary_language)
    TextView mPrimaryLanguageView;
    @Nullable
    @BindView(R.id.parallel_language)
    TextView mParallelLanguageView;

    public static LanguageSettingsFragment newInstance() {
        return new LanguageSettingsFragment();
    }

    /* BEGIN lifecycle */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateLanguages();
    }

    @Override
    protected void onUpdatePrimaryLanguage() {
        super.onUpdatePrimaryLanguage();
        updateLanguages();
    }

    @Override
    protected void onUpdateParallelLanguage() {
        super.onUpdateParallelLanguage();
        updateLanguages();
    }

    /* END lifecycle */

    private void updateLanguages() {
        if (mPrimaryLanguageView != null) {
            mPrimaryLanguageView.setText(LocaleUtils.getDisplayName(mPrimaryLanguage));
        }
        if (mParallelLanguageView != null) {
            if (mParallelLanguage != null) {
                mParallelLanguageView.setText(LocaleUtils.getDisplayName(mParallelLanguage));
            } else {
                mParallelLanguageView.setText(R.string.action_language_parallel_select);
            }
        }
    }

    @Optional
    @OnClick(R.id.primary_language)
    void editPrimaryLanguage() {
        LanguageSelectionActivity.start(requireActivity(), true);
    }

    @Optional
    @OnClick(R.id.parallel_language)
    void editParallelLanguage() {
        LanguageSelectionActivity.start(requireActivity(), false);
    }
}
