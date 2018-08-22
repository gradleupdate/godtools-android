package org.godtools.articles;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.godtools.articles.aem.db.ArticleDao;
import org.godtools.articles.aem.db.ArticleRoomDatabase;
import org.godtools.articles.aem.db.AttachmentDao;
import org.godtools.articles.aem.db.ManifestAssociationDao;
import org.godtools.articles.aem.model.Article;
import org.godtools.articles.aem.model.Attachment;
import org.godtools.articles.aem.model.ManifestAssociation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private ArticleDao mArticleDao;
    private AttachmentDao mAttachmentDao;
    private ManifestAssociationDao mAssociationDao;
    private ArticleRoomDatabase db;

    private TestDao mTestDao;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        db = Room.inMemoryDatabaseBuilder(context, ArticleRoomDatabase.class).build();
        mArticleDao = db.mArticleDao();
        mAttachmentDao = db.mAttachmentDao();
        mAssociationDao = db.mManifestAssociationDao();
        mTestDao = db.mTestDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
//        Context appContext = InstrumentationRegistry.getTargetContext();

        List<Article> articles = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            Article article = new Article();

            article.mContent = "<p>When one is studying a passage of Scripture, a basic but helpful pattern to follow is the threefold process of observation, interpretation, and application.</p>\\n<p>OBSERVATION</p>\\n<p>This is pretty straightforward: observe what the passage is saying and describing.<br>\\nFirst, read through a portion of Scripture and then go back and make some initial observations about it: Who are the characters? What’s going on? Where is this taking place? Revisit the part of your brain that houses all of that literary criticism stuff your English teacher went on and on about while you doodled with your new four-color ballpoint pen. As a quick refresher, remember to ask yourself some basic who- what-when-where observational questions:</p>\\n<p><i>WHO</i>&nbsp;is speaking? Who is this about? Who are the main characters?</p>\\n<p><i>WHAT</i>&nbsp;is the subject or event covered in the chapter? What do you learn about the people, event, or teaching?</p>\\n<p><i>WHEN</i>&nbsp;do/will the events occur or did/will something happen to someone?</p>\\n<p><i>WHERE</i>&nbsp;did or will this happen? Where was it said?</p>\\n<p><i>WHY</i>&nbsp;is something being said or mentioned? Why would/will this happen? Why at that time and/or to this person/people?</p>\\n<p><i>HOW</i>&nbsp;will it happen? How is it to be done? How is it illustrated?</p>\\n<p>I can never remember the “how,” because it rebelliously begins with an h instead of a w. Maybe it’s the same for you. If so, that’s okay. This is just a general template; it’s not meant to be a science or&nbsp;a straitjacket. Once you get a feel for some of these questions, try to keep an eye out for key words or phrases, repeated words, contrasts and comparisons, and terms of summary and conclusions (“so that,” “for this reason,” and so on).</p>\\n<p>INTERPRETATION</p>\\n<p>Often observation slides right into interpretation. But strictly speaking, observation refers to trying to understand what’s being said, while interpretation refers to understanding the overall meaning.</p>\\n<p>From what you’ve learned in your observation&nbsp;of the text, you are trying to discern a primary meaning of the passage—what the biblical author was seeking to communicate and what God was seeking to communicate through that biblical author. A particularly fruitful way to pull these things out&nbsp;of the passage is to ask questions such as “What sinful, broken, or fallen condition is being addressed or corrected by the passage?” “What is the deeper&nbsp;sin beneath the behavior?” and “What prompted&nbsp;the author to write this passage?” Keeping these questions in mind can help you uncover the primary meaning of the text.</p>\\n<p>APPLICATION</p>\\n<p>So, how does the passage apply to you and to others? And what are some actions you need to take in order to apply God’s Word directly to your life?</p>\\n<p>Let me suggest something here. I think our reflections&nbsp;tend to skip along the surface level of behavior or habits that ";
            article.mDateCreated = "Fri Jun 08 2018 18:55:00 GMT+0000";
            article.mTitle = "Romances wolves";
            mArticleDao.insertArticle(article);
        }

        List<Article> savedArticles = mArticleDao.getAllArticles();

        for (Article article : savedArticles) {
            // Create an attachment for each
            Attachment attachment = new Attachment();
            attachment.mArticleKey = article.mId;
            attachment.mAttachmentUrl = "https://i.pinimg.com/originals/b9/9e/a0/b99ea0abc5e6bf5f5c67c1797791cc82.jpg";
            mAttachmentDao.insertAttachment(attachment);

            // Create Manifest Association
            ManifestAssociation association = new ManifestAssociation();
            association.mArticleId = article.mId;
            association.mManifestId = String.valueOf(article.mId % 3);  // just need to only have three versions
            association.mManifestName = String.format("Manifest # %s", association.mManifestId);
            mAssociationDao.insertAssociation(association);
        }

        //verify that we are getting bat the Artcles
        Boolean allAttachmentsSaved = true;

        for (Article article : savedArticles) {
            if (mAttachmentDao.getAttachmentsByArticle(article.mId) == null) {
                allAttachmentsSaved = false;
                break;
            }
        }

        // verify that all of the Articles are retr

        assert (savedArticles.size() > 0 & allAttachmentsSaved && mAssociationDao
                .getArticlesByManifestID("0").size() > 0 && mAssociationDao
                .getAssociationByManifestID("1").size() > 0);
    }
}
