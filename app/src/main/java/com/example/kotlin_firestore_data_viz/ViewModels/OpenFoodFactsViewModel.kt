package com.example.kotlin_firestore_data_viz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_firestore_data_viz.network.FoodApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OpenFoodFactsViewModel : ViewModel() {
    private val apiService = FoodApiService.create()

    private val _foodData = MutableStateFlow<FoodState>(FoodState.Loading)
    val foodData: StateFlow<FoodState> = _foodData

    fun fetchProductByBarcode(barcode: String) {
        viewModelScope.launch {
            _foodData.value = FoodState.Loading
            try {
                val response = apiService.getProductByBarcode(barcode)

                when {
                    response.product == null -> {
                        _foodData.value = FoodState.Error("Product not found")
                    }
                    else -> {
                        val product = response.product
                        _foodData.value = FoodState.Success(
                            name = product.getLocalizedName(),
                            brand = product.brands,
                            servingSize = product.serving_size,
                            origin = product.origin_of_ingredients,
                            energy = product.nutriments?.energy_kcal_100g,
                            protein = product.nutriments?.proteins_100g,
                            carbs = product.nutriments?.carbohydrates_100g,
                            fat = product.nutriments?.fat_100g,
                            sugar = product.nutriments?.sugars_100g,
                            fiber = product.nutriments?.fiber_100g,
                            salt = product.nutriments?.salt_100g
                        )
                    }
                }
            } catch (e: Exception) {
                _foodData.value = FoodState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }
}

sealed class FoodState {
    object Loading : FoodState()
    data class Success(
        val name: String,
        val brand: String?,
        val servingSize: String?,
        val origin: String?,
        val energy: Float?,
        val protein: Float?,
        val carbs: Float?,
        val fat: Float?,
        val sugar: Float?,
        val fiber: Float?,
        val salt: Float?
    ) : FoodState()
    data class Error(val message: String) : FoodState()
}