package com.wlrn566.gpsTracker.Public;

import com.wlrn566.gpsTracker.VO.RestaurantVO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RetrofitAPI {

    // 맛집 로드
    @GET("/load_restaurant.php")
    Call<RestaurantVO> getRestaurantData(
            @Query("topic") String topic
    );

}
