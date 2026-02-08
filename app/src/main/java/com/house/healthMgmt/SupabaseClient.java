package com.house.healthMgmt;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit;

    public static SupabaseApi getApi(Context context) {
        if (retrofit == null) {
            try {
                // 1. assets에서 프로퍼티 파일 읽기
                Properties properties = new Properties();
                AssetManager assetManager = context.getAssets();
                InputStream inputStream = assetManager.open("config.properties");
                properties.load(inputStream);
                
                String url = properties.getProperty("supabase_url");
                String key = properties.getProperty("supabase_key");

                // 2. 헤더를 동적으로 추가하는 OkHttp 클라이언트 생성
                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(chain -> {
                            Request original = chain.request();
                            Request request = original.newBuilder()
                                    .header("ApiKey", key)
                                    .header("Authorization", "Bearer " + key)
                                    .header("Content-Type", "application/json")
                                    .header("Prefer", "return=representation") // 저장 후 결과 받기 위해
                                    .method(original.method(), original.body())
                                    .build();
                            return chain.proceed(request);
                        })
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();

                // 3. Retrofit 빌드
                retrofit = new Retrofit.Builder()
                        .baseUrl(url)
                        .client(client) // 위에서 만든 클라이언트 연결
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return retrofit.create(SupabaseApi.class);
    }
}
