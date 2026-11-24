package pt.isec.amov.cityfix

import android.app.Application
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CategoryRepository
import pt.isec.amov.cityfix.data.OccurrenceRepository
import pt.isec.amov.cityfix.data.UserRepository

class CityFixApp : Application() {
    val authRepository by lazy {
        AuthRepository()
    }

    val occurrenceRepository by lazy {
        OccurrenceRepository(this)
    }

    val categoryRepository by lazy {
        CategoryRepository(this)
    }

    val userRepository by lazy {
        UserRepository(this)
    }
}
