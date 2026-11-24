package pt.isec.amov.cityfix.model

import java.text.Collator
import java.util.Locale

class CategoriesList(initialCategories: List<Category> = emptyList()) {
    private val _categories = initialCategories.toMutableList()

    fun getAllSorted(): List<Category> {
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
        return _categories.sortedWith { first, second ->
            collator.compare(first.title.trim(), second.title.trim())
        }
    }

    fun getActiveSorted(): List<Category> = getAllSorted().filter { it.active }

    fun findById(id: String): Category? = _categories.find { it.id == id }

    fun add(category: Category) {
        _categories.add(category)
    }

    fun upsert(category: Category) {
        val index = _categories.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            _categories[index] = category
        } else {
            _categories.add(category)
        }
    }

    fun rawList(): List<Category> = _categories.toList()
}
