package org.cru.godtools.article.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.cru.godtools.article.R;
import org.cru.godtools.article.R2;
import org.cru.godtools.article.adapter.ArticleAdapter;
import org.cru.godtools.articles.aem.model.Article;
import org.cru.godtools.articles.aem.view_model.ArticleViewModel;
import org.cru.godtools.base.tool.fragment.BaseToolFragment;

import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import timber.log.Timber;

public class ArticlesFragment extends BaseToolFragment implements ArticleAdapter.Callback {
    public static final String TAG = "ArticlesFragment";
    private static final String MANIFEST_KEY = "manifest-key";

    @Nullable
    @BindView(R2.id.articles_recycler_view)
    RecyclerView mArticlesView;

    ArticleAdapter mAdapter;
    String mManifestKey;

    public static ArticlesFragment newInstance(@NonNull final String code, @NonNull final Locale locale,
                                               String manifestKey) {
        ArticlesFragment fragment = new ArticlesFragment();
        Bundle args = new Bundle();
        populateArgs(args, code, locale);
        args.putString(MANIFEST_KEY, manifestKey);
        fragment.setArguments(args);
        return fragment;
    }

    // region LifeCycle Events
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mManifestKey = getArguments() != null ? getArguments().getString(MANIFEST_KEY) : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_articles, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupArticleRecyclerView();
    }
    //endregion

    /**
     * This method will initialize all of the required data for the RecyclerView
     */
    private void setupArticleRecyclerView() {
        if (mArticlesView != null) {
            mAdapter = new ArticleAdapter();
            mAdapter.setCallbacks(this);
            mAdapter.setToolManifest(mManifest);
            DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(),
                    DividerItemDecoration.VERTICAL);
            itemDecoration.setDrawable(Objects.requireNonNull(getActivity()).getResources()
                    .getDrawable(R.drawable.divider));

            mArticlesView.addItemDecoration(itemDecoration);
            mArticlesView.setAdapter(mAdapter);

            ArticleViewModel viewModel = ArticleViewModel.getInstance(getActivity());



            viewModel.getArticlesByManifest(mManifestKey).observe(this, articles -> {
                // This will be triggered by any change to the database
                mAdapter.setArticles(articles);
            });
        }
    }

    /**
     * This is the Override method from ArticleAdapter that will handle the functionality of an article
     * being selected from the list.
     *
     * @param article the selected Article
     */
    @Override
    public void onArticleSelected(Article article) {
        Timber.tag(TAG).d("You selected \"%s\" as your article", article.mTitle);
    }

    /**
     * This is the Override Method from BaseToolFragment.  This is fired anytime the Manifest Object is
     * updated and will update the adapter of it's changes.
     */
    @Override
    protected void onManifestUpdated() {
        super.onManifestUpdated();
        mAdapter.setToolManifest(mManifest);
    }
}
