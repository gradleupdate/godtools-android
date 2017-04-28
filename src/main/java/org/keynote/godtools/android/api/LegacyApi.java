package org.keynote.godtools.android.api;

import org.keynote.godtools.android.BuildConfig;
import org.keynote.godtools.android.business.GTLanguages;
import org.keynote.godtools.android.business.GTNotificationRegister;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.keynote.godtools.android.utils.Constants.INTERPRETER_HEADER;

public interface LegacyApi {
    String V2 = "v2";
    String AUTH = V2 + "/auth";
    String NOTIFICATION = "notification";
    String NOTIFICATION_UPDATE = NOTIFICATION + "/" + "update";
    String META = V2 + "/meta/all";
    String ENDPOINT_DRAFTS = V2 + "/drafts";
    String ENDPOINT_PACKAGES = V2 + "/packages";
    String ENDPOINT_TRANSLATIONS = V2 + "/translations";

    @Headers(INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION)
    @POST(AUTH + "/{code}")
    Call<ResponseBody> getAuthToken(@Path("code") String code);

    @Headers(INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION)
    @GET(AUTH + "/status")
    Call<ResponseBody> verifyAuthToken(@Header(AUTHORIZATION) String token);

    @Headers(INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION)
    @POST(NOTIFICATION + "/{registrationId}")
    Call<ResponseBody> registerDeviceForNotifications(@Path("registrationId") String regId,
                                                      @Header("deviceId") String deviceId,
                                                      @Header("notificationsOn") boolean enableNotifications);


    @Headers(INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION)
    @POST(NOTIFICATION_UPDATE)
    Call<ResponseBody> updateNotification(@Header(AUTHORIZATION) String token, @Body GTNotificationRegister notificationRegister);

    @Headers(INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION)
    @GET(META)
    Call<GTLanguages> getListOfPackages();

    @Headers(INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION)
    @GET(META + "/{langCode}")
    Call<GTLanguages> getListOfDrafts(@Header(AUTHORIZATION) String token, @Path("langCode") String langCode);

    @Headers(INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION)
    @GET(ENDPOINT_DRAFTS + "/{langCode}")
    Call<ResponseBody> downloadDrafts(@Header(AUTHORIZATION) String token, @Path("langCode") String langCode, @Query("compressed") boolean compressed);

    @Headers({INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION})
    @GET(ENDPOINT_PACKAGES + "/{langCode}")
    Call<ResponseBody> downloadPackages(@Header(AUTHORIZATION) String token, @Path("langCode") String langCode);


    @Headers({INTERPRETER_HEADER + ": " + BuildConfig.INTERPRETER_VERSION,"Accept:application/xml","Content-type:application/xml"})
    @POST(ENDPOINT_TRANSLATIONS + "/{langCode}/{packageCode}")
    Call<ResponseBody> createDraft(@Header(AUTHORIZATION) String token, @Path("langCode") String langCode,
                                   @Path("packageCode") String packageCode, @Query("publish") boolean publish);
}