package com.house.healthMgmt;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers; // [중요] 이 줄이 빠져서 에러가 났습니다.
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApi {

    // --- [1. 단백질 기록 관련] ---
    @POST("/rest/v1/health_protein")
    Call<Void> insertProtein(@Body ProteinLog log);

    @GET("/rest/v1/health_protein?select=*&order=id.asc")
    Call<List<ProteinLog>> getTodayLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_protein")
    Call<Void> deleteProtein(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_protein")
    Call<Void> updateProtein(@Query("id") String idQuery, @Body Map<String, Object> updateFields);


    // --- [2. 음식 종류 관리] ---
    @GET("/rest/v1/health_food_type?select=*&order=id.asc")
    Call<List<FoodType>> getFoodTypes();

    @POST("/rest/v1/health_food_type")
    Call<Void> insertFoodType(@Body FoodType food);

    @DELETE("/rest/v1/health_food_type")
    Call<Void> deleteFoodType(@Query("id") String idQuery);


    // --- [3. 체중 관리] ---
    @POST("/rest/v1/health_weight")
    Call<Void> insertWeight(@Body WeightLog log);

    @GET("/rest/v1/health_weight?select=*&order=record_date.desc,created_at.desc&limit=1")
    Call<List<WeightLog>> getLatestWeight();

    @GET("/rest/v1/health_weight?select=*&order=record_date.desc,created_at.desc")
    Call<List<WeightLog>> getWeightLogs();


    // --- [4. 일일 요약 (대시보드 O/X)] ---
    @Headers("Prefer: resolution=merge-duplicates")
    @POST("/rest/v1/health_daily_summary")
    Call<Void> upsertDailySummary(@Body DailySummary summary);
	
	@POST("/rest/v1/health_sodium")
    Call<Void> insertSodium(@Body SodiumLog log);

    @GET("/rest/v1/health_sodium?select=*&order=id.asc")
    Call<List<SodiumLog>> getTodaySodiumLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_sodium")
    Call<Void> deleteSodium(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_sodium")
    Call<Void> updateSodium(@Query("id") String idQuery, @Body Map<String, Object> updateFields);
	
	@POST("/rest/v1/health_water")
    Call<Void> insertWater(@Body WaterLog log);

    @GET("/rest/v1/health_water?select=*&order=id.asc")
    Call<List<WaterLog>> getTodayWaterLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_water")
    Call<Void> deleteWater(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_water")
    Call<Void> updateWater(@Query("id") String idQuery, @Body Map<String, Object> updateFields);
	
	@GET("/rest/v1/health_water_type?select=*&order=id.asc")
    Call<List<WaterType>> getWaterTypes();

    @POST("/rest/v1/health_water_type")
    Call<Void> insertWaterType(@Body WaterType type);

    @DELETE("/rest/v1/health_water_type")
    Call<Void> deleteWaterType(@Query("id") String idQuery);
}
