package org.cru.godtools.model;

import org.ccci.gto.android.common.jsonapi.annotation.JsonApiAttribute;
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiIgnore;
import org.ccci.gto.android.common.jsonapi.annotation.JsonApiType;

import androidx.annotation.Nullable;

import static org.cru.godtools.model.Attachment.JSON_API_TYPE;

@JsonApiType(JSON_API_TYPE)
public class Attachment extends Base {
    static final String JSON_API_TYPE = "attachment";
    private static final String JSON_RESOURCE = "resource";
    private static final String JSON_FILE_NAME = "file-file-name";
    private static final String JSON_SHA256 = "sha256";

    @Nullable
    @JsonApiIgnore
    private Long mToolId;
    @Nullable
    @JsonApiAttribute(name = JSON_RESOURCE)
    private Tool mTool;

    @Nullable
    @JsonApiAttribute(name = JSON_FILE_NAME)
    private String mFileName;
    @Nullable
    @JsonApiAttribute(name = JSON_SHA256)
    private String mSha256;

    @JsonApiIgnore
    private boolean mDownloaded = false;

    public long getToolId() {
        return mToolId != null && mToolId != Tool.INVALID_ID ? mToolId :
                mTool != null ? mTool.getId() : Tool.INVALID_ID;
    }

    public void setToolId(@Nullable final Long id) {
        mToolId = id;
    }

    @Nullable
    public String getFileName() {
        return mFileName;
    }

    public void setFileName(@Nullable final String name) {
        mFileName = name;
    }

    @Nullable
    public String getSha256() {
        return mSha256;
    }

    public void setSha256(@Nullable final String sha256) {
        mSha256 = sha256;
    }

    @Nullable
    public String getLocalFileName() {
        return getLocalFileName(mSha256, mFileName);
    }

    @Nullable
    public static String getLocalFileName(@Nullable final String sha256, @Nullable final String fileName) {
        if (sha256 != null) {
            final int extensionIndex = fileName != null ? fileName.lastIndexOf('.') : -1;
            final String extension = extensionIndex != -1 ? fileName.substring(extensionIndex) : ".bin";
            return sha256 + extension;
        }
        return null;
    }

    public boolean isDownloaded() {
        return mDownloaded;
    }

    public void setDownloaded(final boolean state) {
        mDownloaded = state;
    }
}
