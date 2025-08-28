package com.example.kotlin_firestore_data_viz.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FoodApiService {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name_fi,product_name,brands," +
                "nutriments,serving_size,origin_of_ingredients"
    ): OpenFoodFactsResponse

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(): FoodApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FoodApiService::class.java)
        }
    }
}

data class OpenFoodFactsResponse(
    val status: Int,
    val product: Product?
) {
    data class Product(
        val product_name_fi: String?,
        val product_name: String?,
        val brands: String?,
        val nutriments: Nutriments?,
        val serving_size: String?,
        val origin_of_ingredients: String?
    ) {
        fun getLocalizedName(): String {
            return product_name_fi ?: product_name ?: "Unknown"
        }
    }

    data class Nutriments(
        val energy_kcal_100g: Float?,
        val proteins_100g: Float?,
        val carbohydrates_100g: Float?,
        val fat_100g: Float?,
        val sugars_100g: Float?,
        val fiber_100g: Float?,
        val salt_100g: Float?
    )
}