package com.example.kotlin_firestore_data_viz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_firestore_data_viz.network.FoodApiService
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OpenFoodFactsViewModel : ViewModel() {
    private val api = FoodApiService.create()

    private val _foodData = MutableStateFlow<FoodState>(FoodState.Loading)
    val foodData: StateFlow<FoodState> = _foodData

    fun fetchProductByBarcode(barcode: String) {
        viewModelScope.launch {
            _foodData.value = FoodState.Loading
            try {
                // lean call first
                val resp = api.getProductByBarcode(barcode)
                val p = resp.product
                if (p == null) {
                    _foodData.value = FoodState.Error("Product not found")
                    return@launch
                }

                val nutrients = parseNutrientsRaw(p.nutriments)

                _foodData.value = FoodState.Success(
                    name = p.getLocalizedName(),
                    brand = p.brands,
                    servingSize = p.serving_size,
                    origin = p.origin_of_ingredients,

                    // fully dynamic: (key, value) straight from OFF, only > 0
                    nutrients100g = nutrients,

                    // e-codes / allergens straight from OFF
                    additivesTags = (p.additives_tags ?: emptyList()).map { normalizeECode(it) },
                    allergensText = p.allergens.orElseEmpty(),
                    allergensTags = p.allergens_tags ?: emptyList(),

                    // raw ingredients text (optional to show)
                    ingredientsText = p.ingredients_text
                        ?: p.ingredients_text_fi
                        ?: p.ingredients_text_en
                        ?: ""
                )
            } catch (e: Exception) {
                _foodData.value = FoodState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    // keep key exactly as OFF returns (e.g., "sugars_100g"), include ONLY numeric > 0
    private fun parseNutrientsRaw(obj: JsonObject?): List<NutrientKV> {
        if (obj == null) return emptyList()
        val out = mutableListOf<NutrientKV>()
        // do NOT sort or rename; just forward what exists
        for ((key, el) in obj.entrySet()) {
            if (!key.endsWith("_100g", ignoreCase = true)) continue
            val v = el.asFloatOrNull() ?: continue
            if (v > 0f) out += NutrientKV(key = key, value = v)
        }
        return out
    }

    private fun JsonElement.asFloatOrNull(): Float? = try {
        when {
            isJsonNull -> null
            isJsonPrimitive && asJsonPrimitive.isNumber -> asFloat
            isJsonPrimitive && asJsonPrimitive.isString -> asString.toFloatOrNull()
            else -> null
        }
    } catch (_: Exception) { null }

    private fun normalizeECode(tag: String): String {
        // "en:e330" -> "E330", "e211" -> "E211"
        val raw = tag.substringAfter(':', tag).uppercase()
        return if (raw.startsWith("E")) raw else "E$raw"
    }

    private fun String?.orElseEmpty() = this ?: ""
}

data class NutrientKV(
    val key: String,   // raw OFF key (e.g., "sugars_100g")
    val value: Float   // numeric value per 100g
)

sealed class FoodState {
    object Loading : FoodState()
    data class Success(
        val name: String,
        val brand: String?,
        val servingSize: String?,
        val origin: String?,

        // dynamic nutrients (no renaming, no picking)
        val nutrients100g: List<NutrientKV>,

        // OFF E-codes/allergens unchanged
        val additivesTags: List<String>,
        val allergensText: String,
        val allergensTags: List<String>,

        val ingredientsText: String
    ) : FoodState()
    data class Error(val message: String) : FoodState()
}
