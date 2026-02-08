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

    // --- [1. 단백질 기록 관련 API] ---

    @POST("/rest/v1/health_protein")
    Call<Void> insertProtein(@Body ProteinLog log);

    @GET("/rest/v1/health_protein?select=*&order=id.asc")
    Call<List<ProteinLog>> getTodayLogs(@Query("record_date") String dateQuery);

    @DELETE("/rest/v1/health_protein")
    Call<Void> deleteProtein(@Query("id") String idQuery);

    @PATCH("/rest/v1/health_protein")
    Call<Void> updateProtein(@Query("id") String idQuery, @Body Map<String, Object> updateFields);


    // --- [2. 음식 종류 관리 API (이 부분이 없어서 에러가 났습니다)] ---

    @GET("/rest/v1/health_food_type?select=*&order=id.asc")
    Call<List<FoodType>> getFoodTypes();

    @POST("/rest/v1/health_food_type")
    Call<Void> insertFoodType(@Body FoodType food);

    @DELETE("/rest/v1/health_food_type")
    Call<Void> deleteFoodType(@Query("id") String idQuery);
	
	// --- [체중 관리 API 추가] ---
    @POST("/rest/v1/health_weight")
    Call<Void> insertWeight(@Body WeightLog log);

    // 가장 최근 체중 1개만 가져오기 (날짜 내림차순 정렬)
    @GET("/rest/v1/health_weight?select=*&order=record_date.desc,created_at.desc&limit=1")
    Call<List<WeightLog>> getLatestWeight();
	
	@GET("/rest/v1/health_weight?select=*&order=record_date.desc,created_at.desc")
    Call<List<WeightLog>> getWeightLogs();
}
