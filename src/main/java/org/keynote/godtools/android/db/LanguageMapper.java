package org.keynote.godtools.android.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.keynote.godtools.android.model.Language;

import static org.keynote.godtools.android.db.Contract.LanguageTable.COLUMN_ADDED;
import static org.keynote.godtools.android.db.Contract.LanguageTable.COLUMN_LOCALE;

final class LanguageMapper extends BaseMapper<Language> {
    @Override
    protected void mapField(@NonNull final ContentValues values, @NonNull final String field,
                            @NonNull final Language language) {
        switch (field) {
            case COLUMN_LOCALE:
                values.put(field, serialize(language.getLocale()));
                break;
            case COLUMN_ADDED:
                values.put(field, language.isAdded());
                break;
            default:
                super.mapField(values, field, language);
                break;
        }
    }

    @NonNull
    @Override
    protected Language newObject(@NonNull final Cursor c) {
        return new Language();
    }

    @NonNull
    @Override
    public Language toObject(@NonNull final Cursor c) {
        final Language language = super.toObject(c);

        language.setLocale(getLocale(c, COLUMN_LOCALE, null));
        language.setAdded(getBool(c, COLUMN_ADDED, false));

        return language;
    }
}