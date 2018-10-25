package org.cru.godtools.articles.aem.service.support;

import android.net.Uri;

import org.ccci.gto.android.common.util.IOUtils;
import org.cru.godtools.articles.aem.model.Article;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class AemJsonParserTest {
    @Test
    public void verifyArticleParseLogic() throws Exception {
        final JSONObject jsonObject = loadJson("tests/article-test.json");

        final List<Article> articles = AemJsonParser.findArticles(Uri.parse(
                "https://stage.cru.org/content/experience-fragments/questions_about_god/english"), jsonObject).toList();
        assertThat(articles.size(), is(2));
    }

    private JSONObject loadJson(@NonNull final String file) throws Exception {
        final InputStream data = getInstrumentation().getContext().getAssets().open(file);
        return new JSONObject(IOUtils.readString(data));
    }
}
