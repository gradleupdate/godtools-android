package org.keynote.godtools.android.business;

import android.content.Context;

import org.keynote.godtools.android.dao.DBAdapter;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public class GTLanguage implements Serializable {

    public static final String KEY_PRIMARY = "languagePrimary";
    public static final String KEY_PARALLEL = "languageParallel";

    private long id;
    private String languageName;
    private String languageCode;
    private boolean downloaded;
    private boolean draft;
    private List<GTPackage> listPackages;

    public GTLanguage() {
    }

    public GTLanguage(String languageCode) {
        this.languageCode = languageCode;

        Locale locale = new Locale(languageCode);
        String name = locale.getDisplayName();

        //for some reason not all codes seem to work across all phones
        if ("am-et".equals(locale.getDisplayName())) name = "amharic";
        if ("mn-mn".equals(locale.getDisplayName())) name = "mongolian";

        this.languageName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLanguageName() {
        return languageName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public List<GTPackage> getPackages() {
        return listPackages;
    }

    public void setPackages(List<GTPackage> listPackages) {
        this.listPackages = listPackages;
    }

    public boolean isDraft()
    {
        return draft;
    }

    public void setDraft(boolean draft)
    {
        this.draft = draft;
    }

    public static GTLanguage getLanguage(Context context, String languageCode) {
        DBAdapter adapter = DBAdapter.getInstance(context);
        adapter.open();
        return adapter.getGTLanguage(languageCode);
    }

    public static List<GTLanguage> getAll(Context context) {
        DBAdapter adapter = DBAdapter.getInstance(context);
        adapter.open();
        return adapter.getAllLanguages();
    }

    public long addToDatabase(Context context) {
        DBAdapter adapter = DBAdapter.getInstance(context);
        adapter.open();
        return adapter.insertGTLanguage(this);
    }

    public void update(Context context) {
        DBAdapter adapter = DBAdapter.getInstance(context);
        adapter.open();
        adapter.updateGTLanguage(this);
    }

}
