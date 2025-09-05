package com.example.kotlin_firestore_data_viz.network

import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FoodApiService {

    // Lean call with useful fields; if a product is sparse, you can still call getProductFull.
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("lc") lc: String = "fi",
        @Query("fields") fields: String = "product_name_fi,product_name,brands," +
                "nutriments,serving_size,origin_of_ingredients," +
                "additives_tags,additives_original_tags,additives_old_tags,additives_n," +
                "allergens,allergens_tags," +
                "ingredients_text,ingredients_text_fi,ingredients_text_en," +
                "ingredients,ingredients_tags"
    ): OpenFoodFactsResponse

    // Full call (no field filter) – use only when the lean call gave nothing useful.
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductFull(
        @Path("barcode") barcode: String,
        @Query("lc") lc: String = "fi"
    ): OpenFoodFactsResponse

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"
        fun create(): FoodApiService =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FoodApiService::class.java)
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
        val nutriments: JsonObject?,              // <— dynamic JSON, not a fixed data class
        val serving_size: String?,
        val origin_of_ingredients: String?,

        // Additives / allergens
        val additives_tags: List<String>?,
        val additives_original_tags: List<String>?,
        val additives_old_tags: List<String>?,
        val additives_n: Int?,
        val allergens: String?,
        val allergens_tags: List<String>?,

        // Ingredients (optional to show raw)
        val ingredients_text: String?,
        val ingredients_text_fi: String?,
        val ingredients_text_en: String?,

        // Structured ingredients (not required for this screen, but kept)
        val ingredients: List<Ingredient>?,
        val ingredients_tags: List<String>?
    ) {
        fun getLocalizedName(): String = product_name_fi ?: product_name ?: "Unknown"
    }

    data class Ingredient(
        val id: String? = null,           // e.g., "en:e330" or "en:citric-acid"
        val text: String? = null,         // free text
        val e_number: String? = null      // often "e330", "e415", etc.
    )
}
