package com.house.healthMgmt;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApi {

    // --- [1. 체중 관련] ---
    @GET("/rest/v1/health_weight?select=*&order=record_date.desc&limit=1")
    Call<List<WeightLog>> getLatestWeight();

    @POST("/rest/v1/health_weight")
    Call<Void> insertWeight(@Body WeightLog log);

    @GET("/rest/v1/health_weight?select=*&order=record_date.desc")
    Call<List<WeightLog>> getWeightLogs();

    // --- [2. 음식/물 종류 관리] ---
    @GET("/rest/v1/health_food_type?select=*&order=id.asc")
    Call<List<FoodType>> getFoodTypes();

    @POST("/rest/v1/health_food_type")
    Call<Void> insertFoodType(@Body FoodType type);

    @DELETE("/rest/v1/health_food_type")
    Call<Void> deleteFoodType(@Query("id") String idQuery);

    @GET("/rest/v1/health_water_type?select=*&order=id.asc")
    Call<List<WaterType>> getWaterTypes();

    @POST("/rest/v1/health_water_type")
    Call<Void> insertWaterType(@Body WaterType type);

    @DELETE("/rest/v1/health_water_type")
    Call<Void> deleteWaterType(@Query("id") String idQuery);

    // --- [3. 단백질 기록] ---
    @POST("/rest/v1/health_protein")
    Call<Void> insertProtein(@Body ProteinLog log);

    @GET("/rest/v1/health_protein?select=*&order=id.asc")
    Call<List<ProteinLog>> getTodayLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_protein")
    Call<Void> deleteProtein(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_protein")
    Call<Void> updateProtein(@Query("id") String idQuery, @Body Map<String, Object> updateFields);

    // --- [4. 나트륨 기록] ---
    @POST("/rest/v1/health_sodium")
    Call<Void> insertSodium(@Body SodiumLog log);

    @GET("/rest/v1/health_sodium?select=*&order=id.asc")
    Call<List<SodiumLog>> getTodaySodiumLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_sodium")
    Call<Void> deleteSodium(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_sodium")
    Call<Void> updateSodium(@Query("id") String idQuery, @Body Map<String, Object> updateFields);

    // --- [5. 물 기록] ---
    @POST("/rest/v1/health_water")
    Call<Void> insertWater(@Body WaterLog log);

    @GET("/rest/v1/health_water?select=*&order=id.asc")
    Call<List<WaterLog>> getTodayWaterLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_water")
    Call<Void> deleteWater(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_water")
    Call<Void> updateWater(@Query("id") String idQuery, @Body Map<String, Object> updateFields);

    // --- [6. 음료수(NoBeverage) 기록] --- (이 부분이 없어서 에러 발생했음)
    @POST("/rest/v1/health_beverage")
    Call<Void> insertBeverage(@Body BeverageLog log);

    @GET("/rest/v1/health_beverage?select=*&order=id.asc")
    Call<List<BeverageLog>> getTodayBeverageLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_beverage")
    Call<Void> deleteBeverage(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_beverage")
    Call<Void> updateBeverage(@Query("id") String idQuery, @Body Map<String, Object> updateFields);
	
	@GET("/rest/v1/health_beverage_type?select=*&order=id.asc")
    Call<List<BeverageType>> getBeverageTypes();

    @POST("/rest/v1/health_beverage_type")
    Call<Void> insertBeverageType(@Body BeverageType type);

    @DELETE("/rest/v1/health_beverage_type")
    Call<Void> deleteBeverageType(@Query("id") String idQuery);
	
	@POST("/rest/v1/health_alcohol")
    Call<Void> insertAlcohol(@Body AlcoholLog log);

    @GET("/rest/v1/health_alcohol?select=*&order=id.asc")
    Call<List<AlcoholLog>> getTodayAlcoholLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_alcohol")
    Call<Void> deleteAlcohol(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_alcohol")
    Call<Void> updateAlcohol(@Query("id") String idQuery, @Body Map<String, Object> updateFields);
	
	@GET("/rest/v1/health_alcohol_type?select=*&order=id.asc")
    Call<List<AlcoholType>> getAlcoholTypes();

    @POST("/rest/v1/health_alcohol_type")
    Call<Void> insertAlcoholType(@Body AlcoholType type);

    @DELETE("/rest/v1/health_alcohol_type")
    Call<Void> deleteAlcoholType(@Query("id") String idQuery);
	
	@POST("/rest/v1/health_sleep")
    Call<Void> insertSleep(@Body SleepLog log);

    @GET("/rest/v1/health_sleep?select=*&order=id.asc")
    Call<List<SleepLog>> getTodaySleepLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_sleep")
    Call<Void> deleteSleep(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_sleep")
    Call<Void> updateSleep(@Query("id") String idQuery, @Body Map<String, Object> updateFields);
	
	@GET("/rest/v1/health_sleep_type?select=*&order=id.asc")
    Call<List<SleepType>> getSleepTypes();

    @POST("/rest/v1/health_sleep_type")
    Call<Void> insertSleepType(@Body SleepType type);

    @DELETE("/rest/v1/health_sleep_type")
    Call<Void> deleteSleepType(@Query("id") String idQuery);
	
	@POST("/rest/v1/health_exercise")
    Call<Void> insertExercise(@Body ExerciseLog log);

    @GET("/rest/v1/health_exercise?select=*&order=id.asc")
    Call<List<ExerciseLog>> getTodayExerciseLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_exercise")
    Call<Void> deleteExercise(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_exercise")
    Call<Void> updateExercise(@Query("id") String idQuery, @Body Map<String, Object> updateFields);

    // 운동 종류 API
    @GET("/rest/v1/health_exercise_type?select=*&order=id.asc")
    Call<List<ExerciseType>> getExerciseTypes();
	
	@POST("/rest/v1/health_exercise_type")
    Call<Void> insertExerciseType(@Body ExerciseType type);

    @DELETE("/rest/v1/health_exercise_type")
    Call<Void> deleteExerciseType(@Query("id") String idQuery);
}
