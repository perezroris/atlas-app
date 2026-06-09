package com.atlas.data.api

import com.atlas.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val firmsApi: FirmsApi by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.FIRMS_BASE_URL)
            .client(httpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(FirmsApi::class.java)
    }

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.WEATHER_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

   
    val nominatimApi: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }

    val mapboxDirectionsApi: MapboxDirectionsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.mapbox.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MapboxDirectionsApi::class.java)
    }
}
