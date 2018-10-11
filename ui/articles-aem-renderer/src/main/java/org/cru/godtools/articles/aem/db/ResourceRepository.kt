package org.cru.godtools.articles.aem.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Transaction
import org.cru.godtools.articles.aem.model.Article
import org.cru.godtools.articles.aem.model.Resource

@Dao
abstract class ResourceRepository internal constructor(private val mDb: ArticleRoomDatabase) {
    @Transaction
    open fun storeResources(article: Article, resources: List<Resource>) {
        val articleDao = mDb.articleDao()
        val resourceDao = mDb.resourceDao()

        resources.forEach {
            resourceDao.insertOrIgnore(it)
            articleDao.insertOrIgnore(Article.ArticleResource(article, it))
        }
        articleDao.removeOldResources(article.uri, resources.map { it.uri })
    }
}