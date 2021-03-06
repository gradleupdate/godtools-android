package org.cru.godtools.download.manager;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.common.io.Closer;
import com.google.common.io.CountingInputStream;
import com.google.common.util.concurrent.ListenableFuture;

import org.ccci.gto.android.common.concurrent.NamedThreadFactory;
import org.ccci.gto.android.common.db.Expression;
import org.ccci.gto.android.common.db.Query;
import org.ccci.gto.android.common.eventbus.task.EventBusDelayedPost;
import org.ccci.gto.android.common.util.IOUtils;
import org.ccci.gto.android.common.util.IOUtils.ProgressCallback;
import org.cru.godtools.api.AttachmentsApi;
import org.cru.godtools.api.TranslationsApi;
import org.cru.godtools.base.Settings;
import org.cru.godtools.base.util.FileUtils;
import org.cru.godtools.base.util.PriorityRunnable;
import org.cru.godtools.model.Attachment;
import org.cru.godtools.model.Language;
import org.cru.godtools.model.LocalFile;
import org.cru.godtools.model.Tool;
import org.cru.godtools.model.Translation;
import org.cru.godtools.model.TranslationFile;
import org.cru.godtools.model.TranslationKey;
import org.cru.godtools.model.event.AttachmentUpdateEvent;
import org.cru.godtools.model.event.LanguageUpdateEvent;
import org.cru.godtools.model.event.ToolUpdateEvent;
import org.cru.godtools.model.event.TranslationUpdateEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.keynote.godtools.android.db.Contract.AttachmentTable;
import org.keynote.godtools.android.db.Contract.LanguageTable;
import org.keynote.godtools.android.db.Contract.LocalFileTable;
import org.keynote.godtools.android.db.Contract.ToolTable;
import org.keynote.godtools.android.db.Contract.TranslationFileTable;
import org.keynote.godtools.android.db.Contract.TranslationTable;
import org.keynote.godtools.android.db.GodToolsDao;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.collection.LongSparseArray;
import androidx.collection.SimpleArrayMap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.ccci.gto.android.common.base.TimeConstants.HOUR_IN_MS;
import static org.ccci.gto.android.common.base.TimeConstants.WEEK_IN_MS;
import static org.ccci.gto.android.common.db.Expression.NULL;
import static org.ccci.gto.android.common.db.Expression.constants;
import static org.ccci.gto.android.common.util.ThreadUtils.getLock;

@Singleton
public final class GodToolsDownloadManager {
    private static final int DOWNLOAD_CONCURRENCY = 4;
    private static final long CLEANER_INTERVAL_IN_MS = HOUR_IN_MS;

    private static final int MSG_CLEAN = 1;
    private static final int MSG_PROGRESS_UPDATE = 2;

    private static final ReadWriteLock LOCK_FILESYSTEM = new ReentrantReadWriteLock();
    private static final LongSparseArray<Object> LOCKS_ATTACHMENTS = new LongSparseArray<>();
    private static final ArrayMap<TranslationKey, Object> LOCKS_TRANSLATION_DOWNLOADS = new ArrayMap<>();
    private static final ArrayMap<String, Object> LOCKS_FILES = new ArrayMap<>();

    private final Context mContext;
    private final AttachmentsApi mAttachmentsApi;
    private final TranslationsApi mTranslationsApi;
    private final GodToolsDao mDao;
    private final EventBus mEventBus;
    final Settings mPrefs;
    private final ThreadPoolExecutor mExecutor;
    final Handler mHandler;

    final LongSparseArray<Boolean> mDownloadingAttachments = new LongSparseArray<>();

    @Inject
    GodToolsDownloadManager(@NonNull final Context context, @NonNull final AttachmentsApi attachmentsApi,
                            @NonNull final TranslationsApi translationsApi, @NonNull final GodToolsDao dao,
                            @NonNull final EventBus eventBus, @NonNull final Settings settings) {
        mContext = context;
        mAttachmentsApi = attachmentsApi;
        mTranslationsApi = translationsApi;
        mDao = dao;
        mEventBus = eventBus;
        mHandler = new Handler(Looper.getMainLooper());
        mPrefs = settings;
        mExecutor = new ThreadPoolExecutor(0, DOWNLOAD_CONCURRENCY, 10, TimeUnit.SECONDS,
                                           new PriorityBlockingQueue<>(11, PriorityRunnable.COMPARATOR),
                                           new NamedThreadFactory(GodToolsDownloadManager.class.getSimpleName()));

        // register with EventBus
        mEventBus.register(this);

        // enqueue an initial clean filesystem task
        enqueueCleanFilesystem();
    }

    // region Lifecycle Events

    @WorkerThread
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onLanguageUpdate(@NonNull final LanguageUpdateEvent event) {
        enqueuePendingPublishedTranslations();
    }

    @WorkerThread
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onToolUpdate(@NonNull final ToolUpdateEvent event) {
        enqueueToolBannerAttachments();
        enqueuePendingPublishedTranslations();
    }

    @WorkerThread
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onTranslationUpdate(@NonNull final TranslationUpdateEvent event) {
        enqueuePendingPublishedTranslations();
    }

    @WorkerThread
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onAttachmentUpdate(@NonNull final AttachmentUpdateEvent event) {
        enqueueStaleAttachments();
    }

    // endregion Lifecycle Events

    @AnyThread
    public void addLanguage(@Nullable final Locale locale) {
        if (locale != null) {
            final Language language = new Language();
            language.setCode(locale);
            language.setAdded(true);
            final ListenableFuture<Integer> update = mDao.updateAsync(language, LanguageTable.COLUMN_ADDED);
            update.addListener(new EventBusDelayedPost(mEventBus, LanguageUpdateEvent.INSTANCE),
                               directExecutor());
        }
    }

    @AnyThread
    public void removeLanguage(@Nullable final Locale locale) {
        if (locale != null && !mPrefs.isLanguageProtected(locale)) {
            // clear the parallel language if it is the language being removed
            if (locale.equals(mPrefs.getParallelLanguage())) {
                mPrefs.setParallelLanguage(null);
            }

            // remove the language from the device
            final Language language = new Language();
            language.setCode(locale);
            language.setAdded(false);
            final ListenableFuture<Integer> update = mDao.updateAsync(language, LanguageTable.COLUMN_ADDED);
            update.addListener(this::pruneStaleTranslations, directExecutor());
            update.addListener(new EventBusDelayedPost(mEventBus, LanguageUpdateEvent.INSTANCE),
                               directExecutor());
        }
    }

    @NonNull
    @AnyThread
    public ListenableFuture<Integer> addTool(@NonNull final String code) {
        final Tool tool = new Tool();
        tool.setCode(code);
        tool.setAdded(true);
        final ListenableFuture<Integer> update = mDao.updateAsync(tool, ToolTable.COLUMN_ADDED);
        update.addListener(new EventBusDelayedPost(mEventBus, ToolUpdateEvent.INSTANCE), directExecutor());
        return update;
    }

    @AnyThread
    public void removeTool(@NonNull final String code) {
        final Tool tool = new Tool();
        tool.setCode(code);
        tool.setAdded(false);
        final ListenableFuture<Integer> update = mDao.updateAsync(tool, ToolTable.COLUMN_ADDED);
        update.addListener(this::pruneStaleTranslations, directExecutor());
        update.addListener(new EventBusDelayedPost(mEventBus, ToolUpdateEvent.INSTANCE), directExecutor());
    }

    @AnyThread
    public void cacheTranslation(@NonNull final String code, @NonNull final Locale locale) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> mDao.streamCompat(
                Query.select(Translation.class)
                        .where(TranslationTable.SQL_WHERE_TOOL_LANGUAGE.args(code, locale)
                                       .and(TranslationTable.SQL_WHERE_PUBLISHED))
                        .orderBy(TranslationTable.SQL_ORDER_BY_VERSION_DESC))
                .findFirst()
                .executeIfPresent(t -> {
                    t.updateLastAccessed();
                    mDao.update(t, TranslationTable.COLUMN_LAST_ACCESSED);
                })
                .map(TranslationKey::new)
                .map(DownloadTranslationRunnable::new)
                .ifPresent(mExecutor::execute));
    }

    @WorkerThread
    void pruneStaleTranslations() {
        mDao.inTransaction(() -> {
            // load the tools and languages that are added to this device
            final Object[] tools = mDao
                    .streamCompat(Query.select(Tool.class).where(ToolTable.FIELD_ADDED.eq(true)))
                    .map(Tool::getCode)
                    .withoutNulls()
                    .toArray();
            final Object[] languages = mDao
                    .streamCompat(Query.select(Language.class).where(LanguageTable.SQL_WHERE_ADDED))
                    .map(Language::getCode)
                    .toArray();

            // remove any translation that is no longer added to this device and hasn't been accessed within the past
            // week
            final Translation translation = new Translation();
            translation.setDownloaded(false);
            int changes = mDao.update(translation, TranslationTable.FIELD_TOOL.notIn(constants(tools))
                    .or(TranslationTable.FIELD_LANGUAGE.notIn(constants(languages)))
                    .and(TranslationTable.FIELD_LAST_ACCESSED.lt(new Date(System.currentTimeMillis() - WEEK_IN_MS)))
                    .and(TranslationTable.SQL_WHERE_DOWNLOADED), TranslationTable.COLUMN_DOWNLOADED);

            // remove any translation we have a newer version of
            final Set<TranslationKey> seen = new ArraySet<>();
            changes += mDao.streamCompat(Query.select(Translation.class)
                                                 .where(TranslationTable.SQL_WHERE_DOWNLOADED)
                                                 .orderBy(TranslationTable.SQL_ORDER_BY_VERSION_DESC))
                    // filter out the newest version of every translation
                    .filterNot(t -> seen.add(new TranslationKey(t)))
                    .peek(t -> {
                        t.setDownloaded(false);
                        mDao.update(t, TranslationTable.COLUMN_DOWNLOADED);
                    })
                    .count();

            // if any translations were updated, send a broadcast
            if (changes > 0) {
                mEventBus.post(TranslationUpdateEvent.INSTANCE);
                enqueueCleanFilesystem();
            }

            return true;
        });
    }

    @WorkerThread
    @SuppressWarnings("checkstyle:RightCurly")
    void downloadAttachment(final long attachmentId) {
        // short-circuit if the resources directory isn't valid
        if (!FileUtils.createGodToolsResourcesDir(mContext)) {
            return;
        }

        synchronized (getLock(LOCKS_ATTACHMENTS, attachmentId)) {
            // short-circuit if attachment doesn't exist
            final Attachment attachment = mDao.find(Attachment.class, attachmentId);
            if (attachment == null) {
                return;
            }

            // short-circuit if we don't have a local filename
            final String fileName = attachment.getLocalFileName();
            if (fileName == null) {
                return;
            }

            final Lock lock = LOCK_FILESYSTEM.readLock();
            try {
                lock.lock();
                synchronized (getLock(LOCKS_FILES, fileName)) {
                    // short-circuit if the attachment is actually downloaded
                    LocalFile localFile = mDao.find(LocalFile.class, fileName);
                    if (attachment.isDownloaded() && localFile != null) {
                        return;
                    }
                    attachment.setDownloaded(false);

                    // we don't have a local file, so download it
                    if (localFile == null) {
                        // create a new local file object
                        localFile = new LocalFile();
                        localFile.setFileName(fileName);

                        try {
                            // download attachment
                            final Response<ResponseBody> response = mAttachmentsApi.download(attachmentId).execute();
                            if (response.isSuccessful()) {
                                final ResponseBody body = response.body();
                                if (body != null) {
                                    processStream(localFile, body.byteStream());

                                    // mark attachment as downloaded
                                    attachment.setDownloaded(true);
                                }
                            }
                        } catch (final IOException ignored) {
                        }
                    }
                    // this file is already available locally, so just mark it as downloaded
                    else {
                        attachment.setDownloaded(true);
                    }

                    // update attachment download state
                    mDao.update(attachment, AttachmentTable.COLUMN_DOWNLOADED);
                    mEventBus.post(AttachmentUpdateEvent.INSTANCE);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @WorkerThread
    public void importAttachment(@NonNull final Attachment attachment, @NonNull final InputStream in)
            throws IOException {
        // short-circuit if the resources directory isn't valid
        if (!FileUtils.createGodToolsResourcesDir(mContext)) {
            return;
        }

        final String fileName = attachment.getLocalFileName();

        final Lock lock = LOCK_FILESYSTEM.readLock();
        try {
            lock.lock();
            synchronized (getLock(LOCKS_FILES, fileName)) {
                // short-circuit if the attachment is actually downloaded
                LocalFile localFile = mDao.find(LocalFile.class, fileName);
                if (attachment.isDownloaded() && localFile != null) {
                    return;
                }
                attachment.setDownloaded(false);

                try {
                    // we don't have a local file, so create it
                    if (localFile == null) {
                        // create a new local file object
                        localFile = new LocalFile();
                        localFile.setFileName(fileName);

                        // process the input stream
                        processStream(localFile, in);
                    }

                    // mark attachment as downloaded
                    attachment.setDownloaded(true);
                } finally {
                    // update attachment download state
                    mDao.update(attachment, AttachmentTable.COLUMN_DOWNLOADED);
                    mEventBus.post(AttachmentUpdateEvent.INSTANCE);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void processStream(@NonNull final LocalFile localFile, @NonNull final InputStream stream)
            throws IOException {
        final Closer closer = Closer.create();
        try {
            // short-circuit if we can't create the local file
            final File file = localFile.getFile(mContext);
            if (file == null) {
                throw new FileNotFoundException(localFile.getFileName() + " (File could not be created)");
            }

            // write file
            final InputStream in = closer.register(new BufferedInputStream(stream));
            final OutputStream os = closer.register(new FileOutputStream(file));
            IOUtils.copy(in, os);
            os.flush();
            os.close();

            // store local file in database
            mDao.updateOrInsert(localFile);
        } catch (final Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    @WorkerThread
    void downloadLatestPublishedTranslation(@NonNull final TranslationKey key) {
        // short-circuit if the resources directory isn't valid
        if (!FileUtils.createGodToolsResourcesDir(mContext)) {
            return;
        }

        // lock translation
        synchronized (getLock(LOCKS_TRANSLATION_DOWNLOADS, key)) {
            // process the most recent published version
            final Query<Translation> query = Query.select(Translation.class)
                    .where(TranslationTable.SQL_WHERE_TOOL_LANGUAGE.args(key.getTool(), key.getLocale())
                                   .and(TranslationTable.SQL_WHERE_PUBLISHED))
                    .orderBy(TranslationTable.COLUMN_VERSION + " DESC")
                    .limit(1);
            final Translation translation = mDao.streamCompat(query).findFirst().orElse(null);

            // only process this translation if it's not already downloaded
            if (translation != null && !translation.isDownloaded()) {
                // track the start of this download
                startProgress(key);

                try {
                    final Response<ResponseBody> response = mTranslationsApi.download(translation.getId()).execute();
                    if (response.isSuccessful()) {
                        final ResponseBody body = response.body();
                        if (body != null) {
                            storeTranslation(translation, body.byteStream(), body.contentLength());

                            // prune any old translations
                            pruneStaleTranslations();
                        }
                    }
                } catch (final IOException ignored) {
                }
            }

            // We always finish the download (even if we didn't start it) because of the following race condition:
            //
            // [1] enqueuePendingPublishedTranslations() loads the list of pending downloads from the database
            // [2] downloadLatestPublishedTranslation() finishes downloading one of the translations loaded by [1]
            // [1] enqueuePendingPublishedTranslations() triggers startProgress() on already downloaded translation
            // [1] downloadLatestPublishedTranslation() short-circuits on the actual download logic
            // [1] we still need to call finishDownload()
            finishDownload(key);
        }
    }

    @WorkerThread
    public void storeTranslation(@NonNull final Translation translation, @NonNull final InputStream zipStream,
                                 final long size) throws IOException {
        // short-circuit if the resources directory isn't valid
        if (!FileUtils.createGodToolsResourcesDir(mContext)) {
            return;
        }

        // lock translation
        final TranslationKey key = new TranslationKey(translation);
        synchronized (getLock(LOCKS_TRANSLATION_DOWNLOADS, key)) {
            // track the start of this download
            startProgress(key);

            final Lock lock = LOCK_FILESYSTEM.readLock();
            try {
                lock.lock();

                // process the download
                processZipStream(translation, zipStream, size);

                // mark translation as downloaded
                translation.setDownloaded(true);
                mDao.update(translation, TranslationTable.COLUMN_DOWNLOADED);
                mEventBus.post(TranslationUpdateEvent.INSTANCE);
            } finally {
                lock.unlock();

                // finish the download
                finishDownload(key);
            }
        }
    }

    /**
     * Process a streaming zip response.
     */
    @WorkerThread
    @GuardedBy("LOCK_FILESYSTEM")
    private void processZipStream(@NonNull final Translation translation, @NonNull final InputStream zipStream,
                                  final long size) throws IOException {
        final TranslationKey translationKey = new TranslationKey(translation);

        final Closer closer = Closer.create();
        try {
            final CountingInputStream count = closer.register(new CountingInputStream(zipStream));
            final ZipInputStream zin = closer.register(new ZipInputStream(new BufferedInputStream(count)));
            final ProgressCallback progressCallback = (s) -> updateProgress(translationKey, count.getCount(), size);

            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                final String fileName = ze.getName();
                synchronized (getLock(LOCKS_FILES, fileName)) {
                    // write the file if it hasn't been downloaded before
                    LocalFile localFile = mDao.find(LocalFile.class, fileName);
                    if (localFile == null) {
                        // create a new local file object
                        localFile = new LocalFile();
                        localFile.setFileName(fileName);

                        // short-circuit if we can't create the local file
                        final File file = localFile.getFile(mContext);
                        if (file == null) {
                            throw new FileNotFoundException(fileName + " (File could not be created)");
                        }

                        // write file
                        final OutputStream os = closer.register(new FileOutputStream(file));
                        IOUtils.copy(zin, os, progressCallback);
                        os.flush();
                        os.close();

                        // store local file in database
                        mDao.updateOrInsert(localFile);
                    }

                    // associate this file with this translation
                    final TranslationFile translationFile = new TranslationFile();
                    translationFile.setTranslation(translation);
                    translationFile.setFile(localFile);
                    mDao.updateOrInsert(translationFile);
                }

                updateProgress(translationKey, count.getCount(), size);
            }
        } catch (final Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    @WorkerThread
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void cleanFilesystem() {
        // short-circuit if the resources directory isn't valid
        if (!FileUtils.createGodToolsResourcesDir(mContext)) {
            return;
        }

        // acquire filesystem lock
        final Lock lock = LOCK_FILESYSTEM.writeLock();
        try {
            lock.lock();

            // remove any TranslationFiles for translations that are no longer downloaded
            mDao.streamCompat(Query.select(TranslationFile.class)
                                      .join(TranslationFileTable.SQL_JOIN_TRANSLATION.type("LEFT")
                                                    .andOn(TranslationTable.SQL_WHERE_DOWNLOADED))
                                      .where(TranslationTable.FIELD_ID.is(Expression.NULL)))
                    .forEach(mDao::delete);

            // delete any LocalFiles that are no longer being used
            mDao.streamCompat(Query.select(LocalFile.class)
                                      .join(LocalFileTable.SQL_JOIN_ATTACHMENT.type("LEFT"))
                                      .join(LocalFileTable.SQL_JOIN_TRANSLATION_FILE.type("LEFT"))
                                      .where(AttachmentTable.FIELD_ID.is(Expression.NULL)
                                                     .and(TranslationFileTable.FIELD_FILE.is(Expression.NULL))))
                    .peek(mDao::delete)
                    .map(f -> f.getFile(mContext))
                    .withoutNulls()
                    .forEach(File::delete);

            // delete any orphaned files
            Optional.ofNullable(FileUtils.getGodToolsResourcesDir(mContext).listFiles())
                    .map(Stream::of).stream().flatMap(s -> s)
                    .filter(f -> mDao.find(LocalFile.class, f.getName()) == null)
                    .forEach(File::delete);
        } finally {
            // release filesystem lock
            lock.unlock();
        }
    }

    @WorkerThread
    void detectMissingFiles() {
        // short-circuit if the resources directory isn't valid
        if (!FileUtils.createGodToolsResourcesDir(mContext)) {
            return;
        }

        // acquire filesystem lock
        final Lock lock = LOCK_FILESYSTEM.writeLock();
        try {
            lock.lock();

            // get the set of all downloaded files
            final Set<File> files = Optional.ofNullable(FileUtils.getGodToolsResourcesDir(mContext).listFiles())
                    .map(Stream::of).stream().flatMap(s -> s)
                    .filter(File::isFile)
                    .collect(Collectors.toSet());

            // check for missing files
            mDao.streamCompat(Query.select(LocalFile.class))
                    .filter(f -> !files.contains(f.getFile(mContext)))
                    .forEach(mDao::delete);
        } finally {
            // release filesystem lock
            lock.unlock();
        }
    }

    // region Download Progress
    private final SimpleArrayMap<TranslationKey, List<OnDownloadProgressUpdateListener>> mDownloadProgressListeners =
            new SimpleArrayMap<>();
    private final SimpleArrayMap<TranslationKey, MutableLiveData<DownloadProgress>> mDownloadingProgressLiveData =
            new SimpleArrayMap<>();

    @NonNull
    @AnyThread
    private MutableLiveData<DownloadProgress> getDownloadProgressLiveData(@NonNull final TranslationKey translation) {
        synchronized (mDownloadingProgressLiveData) {
            MutableLiveData<DownloadProgress> liveData = mDownloadingProgressLiveData.get(translation);
            if (liveData == null) {
                liveData = new DownloadProgressLiveData();
                mDownloadingProgressLiveData.put(translation, liveData);
            }
            return liveData;
        }
    }

    @NonNull
    @MainThread
    public LiveData<DownloadProgress> getDownloadProgressLiveData(
            @NonNull final String tool,
            @NonNull final Locale locale
    ) {
        return getDownloadProgressLiveData(new TranslationKey(tool, locale));
    }

    private void startProgress(@NonNull final TranslationKey translation) {
        getDownloadProgressLiveData(translation).postValue(DownloadProgress.INITIAL);
        scheduleProgressUpdate(translation);
    }

    @AnyThread
    private void updateProgress(@NonNull final TranslationKey translation, final long progress, final long max) {
        getDownloadProgressLiveData(translation).postValue(new DownloadProgress(progress, max));
        scheduleProgressUpdate(translation);
    }

    @AnyThread
    private void finishDownload(@NonNull final TranslationKey translation) {
        getDownloadProgressLiveData(translation).postValue(null);
        scheduleProgressUpdate(translation);
    }

    @Nullable
    @AnyThread
    public DownloadProgress getDownloadProgress(@NonNull final String tool, @NonNull final Locale locale) {
        return getDownloadProgressLiveData(new TranslationKey(tool, locale)).getValue();
    }

    @AnyThread
    void scheduleProgressUpdate(@NonNull final TranslationKey translation) {
        // remove any pending executions
        mHandler.removeMessages(MSG_PROGRESS_UPDATE, translation);

        // schedule another execution
        final Message m = Message.obtain(mHandler, () -> dispatchOnProgressUpdateCallbacks(translation));
        m.what = MSG_PROGRESS_UPDATE;
        m.obj = translation;
        mHandler.sendMessage(m);
    }

    @MainThread
    void dispatchOnProgressUpdateCallbacks(@NonNull final TranslationKey translation) {
        // get any listeners
        final List<OnDownloadProgressUpdateListener> listeners = mDownloadProgressListeners.get(translation);

        // dispatch any listeners we have
        if (listeners != null && !listeners.isEmpty()) {
            final DownloadProgress progress = getDownloadProgressLiveData(translation).getValue();

            for (final OnDownloadProgressUpdateListener listener : listeners) {
                listener.onDownloadProgressUpdated(progress);
            }
        }
    }

    @MainThread
    public void addOnDownloadProgressUpdateListener(@NonNull final String tool, @NonNull final Locale locale,
                                                    @NonNull final OnDownloadProgressUpdateListener listener) {
        final TranslationKey key = new TranslationKey(tool, locale);
        List<OnDownloadProgressUpdateListener> listeners = mDownloadProgressListeners.get(key);
        if (listeners == null) {
            listeners = new ArrayList<>();
            mDownloadProgressListeners.put(key, listeners);
        }
        listeners.add(listener);
    }

    @MainThread
    public void removeOnDownloadProgressUpdateListener(@NonNull final String tool, @NonNull final Locale locale,
                                                       @NonNull final OnDownloadProgressUpdateListener listener) {
        final TranslationKey key = new TranslationKey(tool, locale);
        final List<OnDownloadProgressUpdateListener> listeners = mDownloadProgressListeners.get(key);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                mDownloadProgressListeners.remove(key);
            }
        }
        if (listeners == null || listeners.isEmpty()) {
            mDownloadProgressListeners.remove(key);
        }
    }

    @MainThread
    public void removeOnDownloadProgressUpdateListener(@NonNull final OnDownloadProgressUpdateListener listener) {
        for (int i = 0; i < mDownloadProgressListeners.size(); i++) {
            final List<OnDownloadProgressUpdateListener> listeners = mDownloadProgressListeners.valueAt(i);
            if (listeners != null) {
                listeners.remove(listener);
            }
            if (listeners == null || listeners.isEmpty()) {
                mDownloadProgressListeners.removeAt(i);
                i--;
            }
        }
    }
    // endregion Download Progress

    // region Download & Cleaning Scheduling Methods

    @WorkerThread
    private void enqueuePendingPublishedTranslations() {
        final Query<Translation> query = Query.select(Translation.class)
                .joins(TranslationTable.SQL_JOIN_LANGUAGE, TranslationTable.SQL_JOIN_TOOL)
                .where(LanguageTable.SQL_WHERE_ADDED
                               .and(ToolTable.FIELD_ADDED.eq(true))
                               .and(TranslationTable.SQL_WHERE_PUBLISHED)
                               .and(TranslationTable.FIELD_DOWNLOADED.eq(false)))
                .orderBy(TranslationTable.COLUMN_VERSION + " DESC");

        mDao.streamCompat(query)
                .distinctBy(TranslationKey::new)
                .filterNot(Translation::isDownloaded)
                .map(TranslationKey::new)
                .peek(this::startProgress)
                .map(DownloadTranslationRunnable::new)
                .forEach(mExecutor::execute);
    }

    @WorkerThread
    private void enqueueToolBannerAttachments() {
        mDao.streamCompat(Query.select(Attachment.class)
                                  .join(AttachmentTable.SQL_JOIN_TOOL.andOn(
                                          ToolTable.FIELD_DETAILS_BANNER.eq(AttachmentTable.FIELD_ID)
                                                  .or(ToolTable.FIELD_BANNER.eq(AttachmentTable.FIELD_ID))))
                                  .where(AttachmentTable.FIELD_DOWNLOADED.eq(false)))
                .mapToLong(Attachment::getId)
                .forEach(this::enqueueAttachmentDownload);
    }

    @WorkerThread
    private void enqueueStaleAttachments() {
        mDao.streamCompat(Query.select(Attachment.class)
                                  .join(AttachmentTable.SQL_JOIN_LOCAL_FILE.type("LEFT"))
                                  .where(AttachmentTable.SQL_WHERE_DOWNLOADED.and(LocalFileTable.FIELD_NAME.is(NULL))))
                .mapToLong(Attachment::getId)
                .forEach(this::enqueueAttachmentDownload);
    }

    @AnyThread
    private void enqueueAttachmentDownload(final long attachmentId) {
        synchronized (mDownloadingAttachments) {
            if (!mDownloadingAttachments.get(attachmentId, false)) {
                mExecutor.execute(new DownloadAttachmentRunnable(attachmentId));
                mDownloadingAttachments.put(attachmentId, true);
            }
        }
    }

    @AnyThread
    private void enqueueCleanFilesystem() {
        mExecutor.execute(new CleanFileSystem());
    }

    @AnyThread
    void scheduleNextCleanFilesystem() {
        // remove any pending executions
        mHandler.removeMessages(MSG_CLEAN);

        // schedule another execution
        final Message m = Message.obtain(mHandler, GodToolsDownloadManager.this::enqueueCleanFilesystem);
        m.what = MSG_CLEAN;
        mHandler.sendMessageDelayed(m, CLEANER_INTERVAL_IN_MS);
    }

    // endregion Download & Cleaning Scheduling Methods

    public interface OnDownloadProgressUpdateListener {
        /**
         * @param progress The current download progress. If this is null there is no download running.
         */
        void onDownloadProgressUpdated(@Nullable DownloadProgress progress);
    }

    // region Task PriorityRunnables

    private static final int PRIORITY_PRIMARY = -40;
    private static final int PRIORITY_ATTACHMENT = -30;
    private static final int PRIORITY_PARALLEL = -20;
    private static final int PRIORITY_OTHER = -10;
    private static final int PRIMARY_PRUNE_FILESYSTEM = Integer.MAX_VALUE;

    final class DownloadTranslationRunnable implements PriorityRunnable {
        @NonNull
        final TranslationKey mKey;
        final int mPriority;

        DownloadTranslationRunnable(@NonNull final TranslationKey key) {
            mKey = key;
            final Locale primary = mPrefs.getPrimaryLanguage();
            final Locale parallel = mPrefs.getParallelLanguage();
            mPriority = primary.equals(mKey.getLocale()) ? PRIORITY_PRIMARY :
                    mKey.getLocale().equals(parallel) ? PRIORITY_PARALLEL : PRIORITY_OTHER;
        }

        @Override
        public int getPriority() {
            return mPriority;
        }

        @Override
        public void run() {
            downloadLatestPublishedTranslation(mKey);
        }
    }

    final class DownloadAttachmentRunnable implements PriorityRunnable {
        private final long mAttachmentId;

        DownloadAttachmentRunnable(final long attachmentId) {
            mAttachmentId = attachmentId;
        }

        @Override
        public int getPriority() {
            return PRIORITY_ATTACHMENT;
        }

        @Override
        public void run() {
            downloadAttachment(mAttachmentId);
            synchronized (mDownloadingAttachments) {
                mDownloadingAttachments.remove(mAttachmentId);
            }
        }
    }

    final class CleanFileSystem implements PriorityRunnable {
        @Override
        public int getPriority() {
            return PRIMARY_PRUNE_FILESYSTEM;
        }

        @Override
        public void run() {
            detectMissingFiles();
            cleanFilesystem();
            scheduleNextCleanFilesystem();
        }
    }

    // endregion Task PriorityRunnables
}
