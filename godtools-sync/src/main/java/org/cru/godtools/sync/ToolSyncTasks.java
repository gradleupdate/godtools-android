package org.cru.godtools.sync;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SimpleArrayMap;

import org.ccci.gto.android.common.db.Query;
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject;
import org.ccci.gto.android.common.jsonapi.retrofit2.JsonApiParams;
import org.ccci.gto.android.common.jsonapi.util.Includes;
import org.cru.godtools.api.model.ToolViews;
import org.cru.godtools.model.Translation;
import org.keynote.godtools.android.db.Contract.ToolTable;
import org.keynote.godtools.android.model.Tool;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

import static org.ccci.gto.android.common.TimeConstants.DAY_IN_MS;

final class ToolSyncTasks extends BaseDataSyncTasks {
    private static final Object LOCK_SYNC_TOOLS = new Object();
    private static final Object LOCK_SYNC_SHARES = new Object();

    private static final String SYNC_TIME_TOOLS = "last_synced.tools";
    private static final long STALE_DURATION_TOOLS = DAY_IN_MS;

    private static final String INCLUDE_ATTACHMENTS = Tool.JSON_ATTACHMENTS;
    private static final String INCLUDE_LATEST_TRANSLATIONS =
            Tool.JSON_LATEST_TRANSLATIONS + "." + Translation.JSON_LANGUAGE;

    private static final String[] API_GET_INCLUDES = {INCLUDE_ATTACHMENTS, INCLUDE_LATEST_TRANSLATIONS};

    ToolSyncTasks(@NonNull final Context context) {
        super(context);
    }

    boolean syncResources(@NonNull final Bundle args) throws IOException {
        final SimpleArrayMap<Class<?>, Object> events = new SimpleArrayMap<>();

        synchronized (LOCK_SYNC_TOOLS) {
            // short-circuit if we aren't forcing a sync and the data isn't stale
            final boolean force = isForced(args);
            if (!force && System.currentTimeMillis() - mDao.getLastSyncTime(SYNC_TIME_TOOLS) < STALE_DURATION_TOOLS) {
                return true;
            }

            // generate params & includes objects
            final Includes includes = new Includes(API_GET_INCLUDES);
            final JsonApiParams params = new JsonApiParams().include(API_GET_INCLUDES);

            // fetch tools from the API
            // short-circuit if this response is invalid
            final Response<JsonApiObject<Tool>> response = mApi.tools.list(params).execute();
            if (response == null || response.code() != 200) {
                return false;
            }

            // store fetched tools
            final JsonApiObject<Tool> json = response.body();
            if (json != null) {
                final LongSparseArray<Tool> existing = index(mDao.get(Query.select(Tool.class)));
                storeTools(events, json.getData(), existing, includes);
            }

            // send any pending events
            sendEvents(events);

            // update the sync time
            mDao.updateLastSyncTime(SYNC_TIME_TOOLS);
        }

        return true;
    }

    boolean syncShares() {
        synchronized (LOCK_SYNC_SHARES) {
            final List<ToolViews> viewsList =
                    mDao.streamCompat(Query.select(Tool.class).where(ToolTable.SQL_WHERE_HAS_PENDING_SHARES))
                            .map(ToolViews::new)
                            .toList();

            for (final ToolViews views : viewsList) {
                try {
                    final Response<JsonApiObject<ToolViews>> response = mApi.views.submitViews(views).execute();
                    if (response.isSuccessful()) {
                        mDao.updateSharesDelta(views.getToolCode(), 0 - views.getQuantity());
                    }
                } catch (final IOException ignored) {
                }
            }
        }
        return true;
    }
}