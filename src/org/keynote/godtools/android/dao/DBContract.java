package org.keynote.godtools.android.dao;

import android.provider.BaseColumns;

import org.keynote.godtools.android.business.GTLanguage;

public class DBContract {

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String DOUBLE_TYPE = " DOUBLE";
    private static final String DATE_TYPE = " DATE";
    private static final String BOOL_TYPE = " BOOL";
    private static final String PRIMARY_KEY = " PRIMARY KEY";
    private static final String COMMA_SEP = ",";

    public static abstract class GTPackageTable implements BaseColumns {
        public static final String TABLE_NAME = "gtpackages";
        public static final String COL_CODE = "code";
        public static final String COL_NAME = "name";
        public static final String COL_LANGUAGE = "language";
        public static final String COL_VERSION = "version";
        public static final String COL_CONFIG_FILE_NAME = "config_file_name";
        public static final String COL_STATUS = "status";
        public static final String COL_ICON = "icon";


        public static final String SQL_CREATE_GTPACKAGES = "CREATE TABLE "
                + GTPackageTable.TABLE_NAME + "("
                + GTPackageTable._ID + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP
                + GTPackageTable.COL_CODE + TEXT_TYPE + COMMA_SEP
                + GTPackageTable.COL_NAME + TEXT_TYPE + COMMA_SEP
                + GTPackageTable.COL_LANGUAGE + TEXT_TYPE + COMMA_SEP
                + GTPackageTable.COL_CONFIG_FILE_NAME + TEXT_TYPE + COMMA_SEP
                + GTPackageTable.COL_ICON + TEXT_TYPE + COMMA_SEP
                + GTPackageTable.COL_STATUS + TEXT_TYPE + COMMA_SEP
                + GTPackageTable.COL_VERSION + DOUBLE_TYPE + ")";

        public static final String SQL_DELETE_GTPACKAGES = "DROP TABLE IF EXISTS "
                + GTPackageTable.TABLE_NAME;
    }

    public static abstract class GTLanguageTable implements BaseColumns {
        public static final String TABLE_NAME = "gtlanguages";
        public static final String COL_CODE = "code";
        public static final String COL_IS_DOWNLOADED = "is_downloaded";

        public static final String SQL_CREATE_GTLANGUAGES = "CREATE TABLE "
                + GTLanguageTable.TABLE_NAME + "("
                + GTLanguageTable._ID + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP
                + GTLanguageTable.COL_CODE + TEXT_TYPE + COMMA_SEP
                + GTLanguageTable.COL_IS_DOWNLOADED + INTEGER_TYPE + ")";

        public static final String SQL_DELETE_GTLANGUAGES = "DROP TABLE IF EXISTS "
                + GTLanguageTable.TABLE_NAME;
    }

    public static abstract class GTDraftLanguageTable implements BaseColumns {
        public static final String TABLE_NAME = "gtdraftlanguages";
        public static final String COL_CODE = "code";
        public static final String COL_IS_DOWNLOADED = "is_downloaded";

        public static final String SQL_CREATE_GTDRAFTLANGUAGES = "CREATE TABLE "
                + GTLanguageTable.TABLE_NAME + "("
                + GTLanguageTable._ID + INTEGER_TYPE + PRIMARY_KEY + COMMA_SEP
                + GTLanguageTable.COL_CODE + TEXT_TYPE + COMMA_SEP
                + GTLanguageTable.COL_IS_DOWNLOADED + INTEGER_TYPE + ")";

        public static final String SQL_DELETE_GTDRAFTLANGUAGES = "DROP TABLE IF EXISTS "
                + GTLanguageTable.TABLE_NAME;
    }

}
