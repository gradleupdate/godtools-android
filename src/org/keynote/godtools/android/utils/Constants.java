package org.keynote.godtools.android.utils;

/**
 * Created by matthewfrederick on 5/5/15.
 */
public class Constants
{
    /**
     * Used as the key to type of background service for intent extras
     */
    public static final String TYPE = "TYPE";

    /**
     * Used as the key to sender of background service for intent extras
     */
    public static final String SENDER = "SENDER";

    public static final String KEY_NEW_LANGUAGE = "new_language";
    public static final String KEY_UPDATE_PRIMARY = "update_primary";
    public static final String KEY_UPDATE_PARALLEL = "update_parallel";

    public static final String META = "meta";

    public static final String META_IS = "meta_is";
    public static final String META_LANG_CODE = "meta_lang_code";
    public static final String META_TAG = "meta_tag";



    /**
     * App preferences name
     */
    public static final String PREFS_NAME = "GodTools";

    public static final String AUTH_CODE = "Authorization_Generic";

    /** constants used to specify the task that was run. */

    /**
     * Used to specify that the generic authentication task was run
     */
    public static final int AUTHENTICATE_GENERIC = 0;

    public static final int GET_LIST_OF_PACKAGES = 1;
}
