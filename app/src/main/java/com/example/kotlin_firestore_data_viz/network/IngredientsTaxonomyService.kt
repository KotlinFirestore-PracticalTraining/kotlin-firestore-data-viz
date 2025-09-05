package com.example.kotlin_firestore_data_viz.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 * Ingredients taxonomy: https://static.openfoodfacts.org/data/taxonomies/ingredients.json
 * Each key like "en:citric-acid" may include `e_number` (e.g. "e330").
 */
interface IngredientsTaxonomyService {
    @GET("data/taxonomies/ingredients.json")
    suspend fun getIngredients(): Map<String, IngredientNode>

    companion object {
        fun create(): IngredientsTaxonomyService =
            Retrofit.Builder()
                .baseUrl("https://static.openfoodfacts.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(IngredientsTaxonomyService::class.java)
    }
}

data class IngredientNode(
    val e_number: String? = null
)