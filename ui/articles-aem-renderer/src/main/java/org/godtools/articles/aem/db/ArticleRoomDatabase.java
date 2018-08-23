package org.godtools.articles.aem.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import org.godtools.articles.aem.model.Article;
import org.godtools.articles.aem.model.Attachment;
import org.godtools.articles.aem.model.ManifestAssociation;

/**
 * This class is used to create the database table.
 *
 * @author Gyasi Story
 */
@Database(entities = { Article.class, ManifestAssociation.class, Attachment.class }, version = 1)
public abstract class ArticleRoomDatabase extends RoomDatabase {

    public abstract ArticleDao mArticleDao();

    public abstract ManifestAssociationDao mManifestAssociationDao();

    public abstract AttachmentDao mAttachmentDao();

    private static ArticleRoomDatabase instance;

    /**
     * Used to get the current Instance of the Room database.
     *
     * @param context = the application context
     * @return = Instance of the current Room Database
     */
    static ArticleRoomDatabase getINSTANCE(final Context context) {
        synchronized (ArticleRoomDatabase.class) {
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(),
                        ArticleRoomDatabase.class, "article_database")
                        //Todo: Create a Migration for database
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
        return instance;
    }

}
