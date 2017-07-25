package org.keynote.godtools.android;

import android.net.Uri;

public final class Constants {
    // common extras
    public static final String EXTRA_TOOL = org.cru.godtools.base.Constants.EXTRA_TOOL;

    // SharedPreferences for this app
    public static final String PREFS_SETTINGS = "GodTools";
    public static final String PREF_PRIMARY_LANGUAGE = "languagePrimary";
    public static final String PREF_PARALLEL_LANGUAGE = "languageParallel";

    // Common base URI's
    public static final Uri MAILTO_SUPPORT = Uri.parse("mailto:support@godtoolsapp.com");
    public static final Uri URI_HELP = Uri.parse("http://www.godtoolsapp.com/faq");
    public static final Uri URI_PRIVACY = Uri.parse("https://www.cru.org/about/privacy.html");
    public static final Uri URI_TERMS_OF_USE = Uri.parse("http://www.godtoolsapp.com/terms");
    public static final Uri URI_COPYRIGHT = Uri.parse("http://www.godtoolsapp.com/copyright");
    public static final String URI_SHARE_BASE = "http://www.knowgod.com/";
}