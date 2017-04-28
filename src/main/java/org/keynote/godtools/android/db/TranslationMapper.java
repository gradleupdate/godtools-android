package org.keynote.godtools.android.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.keynote.godtools.android.model.Language;
import org.keynote.godtools.android.model.Resource;
import org.keynote.godtools.android.model.Translation;

import static org.keynote.godtools.android.db.Contract.TranslationTable.COLUMN_DOWNLOADED;
import static org.keynote.godtools.android.db.Contract.TranslationTable.COLUMN_LANGUAGE;
import static org.keynote.godtools.android.db.Contract.TranslationTable.COLUMN_PUBLISHED;
import static org.keynote.godtools.android.db.Contract.TranslationTable.COLUMN_RESOURCE;
import static org.keynote.godtools.android.db.Contract.TranslationTable.COLUMN_VERSION;
import static org.keynote.godtools.android.model.Translation.DEFAULT_PUBLISHED;
import static org.keynote.godtools.android.model.Translation.DEFAULT_VERSION;

final class TranslationMapper extends BaseMapper<Translation> {
    @Override
    protected void mapField(@NonNull final ContentValues values, @NonNull final String field,
                            @NonNull final Translation translation) {
        switch (field) {
            case COLUMN_RESOURCE:
                values.put(field, translation.getResourceId());
                break;
            case COLUMN_LANGUAGE:
                values.put(field, serialize(translation.getLanguageCode()));
                break;
            case COLUMN_VERSION:
                values.put(field, translation.getVersion());
                break;
            case COLUMN_PUBLISHED:
                values.put(field, translation.isPublished());
                break;
            case COLUMN_DOWNLOADED:
                values.put(field, translation.isDownloaded());
                break;
            default:
                super.mapField(values, field, translation);
                break;
        }
    }

    @NonNull
    @Override
    protected Translation newObject(@NonNull final Cursor c) {
        return new Translation();
    }

    @NonNull
    @Override
    public Translation toObject(@NonNull final Cursor c) {
        final Translation translation = super.toObject(c);

        translation.setResourceId(getLong(c, COLUMN_RESOURCE, Resource.INVALID_ID));
        translation.setLanguageCode(getLocale(c, COLUMN_LANGUAGE, Language.INVALID_CODE));
        translation.setVersion(getInt(c, COLUMN_VERSION, DEFAULT_VERSION));
        translation.setPublished(getBool(c, COLUMN_PUBLISHED, DEFAULT_PUBLISHED));
        translation.setDownloaded(getBool(c, COLUMN_DOWNLOADED, false));

        return translation;
    }
}
