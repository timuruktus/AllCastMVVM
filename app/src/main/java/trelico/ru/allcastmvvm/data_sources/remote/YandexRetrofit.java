package trelico.ru.allcastmvvm.data_sources.remote;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

interface YandexRetrofit{


    /**
     * curl -X POST \
     *      -H "Authorization: Bearer ${IAM_TOKEN}" \
     *      --data-urlencode "texts=Hello World" \
     *      -d "lang=en-US&folderId=${FOLDER_ID}" \
     *      "https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize" > speech.ogg
     */

    /**
     * POST /speech/v1/tts:synthesize HTTP/1.1
     * Host: tts.api.cloud.yandex.net
     * Authorization: Bearer CggVAgAAABoBMRKABFAsG7ZsE5Ww7C6S7EMHcS2DMDQNI9nkya_6k3g3ZAKrTXxJIsnPTO4jgtMhMENEYUq-l4xUpTlUKWNVovnM4b4Upt47eZSAl2-jJC14XAPDYNmxKCJmWfFDhjEwmPaT50z6YQNHHllquSWkQNo1loZzg-YPRKia90hQnhmcfcoKpi-8YYqVm3WQub1UiUZuzA6l01_diMPaJ_W22h4dpQeaxa8SmGlrirLz7hCNBtwnWisE_rtZ2T4sTzpDIgGHbjRrv2FTFFfZncnLmGZxqgrTcqXVGhAvi4VE3iGGxNF24rvWhPLVX2gtJ6JTnLxSYM58Fl8R-OHcqVbI7nMU2pijFa4IMDj-lsO98N2VvNO3dZP9Qt_dI3yxcE4eKcKIr46cyYYGnHPQOrDt15On5xGh2302t1t_kbl62v4GGoG_XWcoCheRhJaMItdGuyQacAtH2WRqPIn0PJAqvOgg4i8E_ZdwF5TQhuGJZl7ja1grhAPd0V47Q29VABaS0A2VJzJBQWzLNNRZUNJAU4Rnw9nVWH3RJDQ6vCkBxcN9zEGrrlBfhndgi_IGq_oL0DicTEhToinvRKdkGOzMq2x8ex_RC8fRJ1Q32cTFdH3b902Upn1UOlEefIsJxDUocWcii6Fqj9JHp3C59flQGNPjXI7JLkjq4Qhg27iRjUgedAp9Gm8KIGY4NmI0NGViOTI3MDRlNmRhNjhiZTA0MjA3ZGRkZTFlEP_xiuoFGL_DjeoFIjcKFGFqZWQyZHEwcm5kMTRmdGNnMTBvEgdzZXJ2aWNlKhRiMWdhcDY4MmlnY3R2MGY2ZjNhZzACMAU4AVABWgA
     * Content-Type: application/x-www-form-urlencoded
     * User-Agent: PostmanRuntime/7.13.0
     * Accept:
     *Cache-Control:no-cache
     *Postman-Token:705d9459-3919-4d17-8192-57d0c064a181,594dd1db-0ade-4b6f-973d-122fd51976aa
     *Host:tts.api.cloud.yandex.net
     *accept-encoding:gzip,deflate
     *content-length:21
     *Connection:keep-alive
     *cache-control:no-cache
     *texts=Hello%2C+world
     */

    @Streaming
    @FormUrlEncoded
    @Headers({"Content-Type: application/x-www-form-urlencoded","Authorization: Api-Key AQVN1Xr8UfXndfzLL-4xBYOtQ-804TFlPfIhJ0RH"})
    @POST("speech/v1/tts:synthesize")
    Observable<ResponseBody> getSpeechOgg(@Field("text") String text, @Field("emotion") String emotion);


}
