package org.cru.godtools.api;

import org.cru.godtools.api.model.GTNotificationRegister;

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

@Deprecated
public interface LegacyApi {
    String V2 = "v2";
    String AUTH = V2 + "/auth";
    String NOTIFICATION = "notification";
    String NOTIFICATION_UPDATE = NOTIFICATION + "/" + "update";
    String ENDPOINT_TRANSLATIONS = V2 + "/translations";

    String HEADER_INTERPRETER = "interpreter: 1";

    @Headers(HEADER_INTERPRETER)
    @POST(AUTH + "/{code}")
    Call<ResponseBody> getAuthToken(@Path("code") String code);

    @Headers(HEADER_INTERPRETER)
    @GET(AUTH + "/status")
    Call<ResponseBody> verifyAuthToken(@Header(AUTHORIZATION) String token);

    @Headers(HEADER_INTERPRETER)
    @POST(NOTIFICATION + "/{registrationId}")
    Call<ResponseBody> registerDeviceForNotifications(@Path("registrationId") String regId,
                                                      @Header("deviceId") String deviceId,
                                                      @Header("notificationsOn") boolean enableNotifications);

    @Headers(HEADER_INTERPRETER)
    @POST(NOTIFICATION_UPDATE)
    Call<ResponseBody> updateNotification(@Header(AUTHORIZATION) String token,
                                          @Body GTNotificationRegister notificationRegister);

    @Headers({
            HEADER_INTERPRETER,
            "Accept: application/xml",
            "Content-type: application/xml"
    })
    @POST(ENDPOINT_TRANSLATIONS + "/{langCode}/{packageCode}")
    Call<ResponseBody> createDraft(@Header(AUTHORIZATION) String token, @Path("langCode") String langCode,
                                   @Path("packageCode") String packageCode, @Query("publish") boolean publish);
}
