package org.cru.godtools.articles.aem.db;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.cru.godtools.articles.aem.model.Article;
import org.cru.godtools.articles.aem.model.Attachment;
import org.cru.godtools.articles.aem.model.ManifestAssociation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ArticleDBTest {
    private static final String TAG = "ArticleDBTest";

    private ArticleDao mArticleDao;
    private AttachmentDao mAttachmentDao;
    private ManifestAssociationDao mAssociationDao;
    private ArticleRoomDatabase db;

    private List<Article> mSavedArticles = new ArrayList<>();

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        db = Room.inMemoryDatabaseBuilder(context,
                ArticleRoomDatabase.class).allowMainThreadQueries().build();
        mArticleDao = db.articleDao();
        mAttachmentDao = db.attachmentDao();
        mAssociationDao = db.manifestAssociationDao();


        for (int i = 0; i < 12; i++) {
            Article article = new Article();

            article.mContent = "<p> The Body </>";
            try {
                article.mDateCreated = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss zz")
                        .parse("Fri Jun 08 2018 18:55:00 GMT+0000").getTime();
                article.mDateUpdated = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss zz")
                        .parse("Sat May 19 2018 00:23:39 GMT+0000").getTime();
            } catch (ParseException e) {
                Log.e(TAG, "createDb: ", e);
                assert (false);
            }
            Calendar calendar = Calendar.getInstance();
            calendar.getTime();
            calendar.setTimeInMillis(article.mDateCreated);
            Date date = new Date(article.mDateCreated);
            date.getYear();
            date.getDate();
            date.getHours();
            date.getTimezoneOffset();
            article.mTitle = " The title = " + i;
            mArticleDao.insertArticle(article);

        }

        mSavedArticles = mArticleDao.getTestableAllArticles();


        for (int i = 0; i < mSavedArticles.size(); i++) {
            ManifestAssociation association = new ManifestAssociation();
            association.mArticleId = mSavedArticles.get(i).mId;
            association.mManifestName = "Manifest ID " + (i % 3);
            association.mManifestId = (i % 3) + "";
            mAssociationDao.insertAssociation(association);

            Attachment attachment = new Attachment();
            attachment.mArticleKey = mSavedArticles.get(i).mId;
            attachment.mAttachmentUrl =
                    "https://believeacts2blog.files.wordpress.com/2015/10/image16.jpg";
        }

    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void verifyGetAllArticles() {

        assert (mSavedArticles.size() > 1);

    }


    @Test
    public void verifyArticleHasAttachment() {
        Boolean hasAttachment = false;
        for (Article article : mSavedArticles) {
            hasAttachment = mAttachmentDao.getTestableAttachmentsByArticle(article.mId)
                    .size() > 0;
            if (!hasAttachment) {
                break;
            }
        }

        assert (hasAttachment);
    }


    @Test
    public void verifyManifestHasArticles() {

        assert (mAssociationDao.getTestableArticlesByManifestID("0").size() > 0 &&
                mAssociationDao.getTestableArticlesByManifestID("1").size() > 0 &&
                mAssociationDao.getTestableArticlesByManifestID("2").size() > 0);
    }


}
