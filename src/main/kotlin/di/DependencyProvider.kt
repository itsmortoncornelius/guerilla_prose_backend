package di

import com.google.gson.Gson
import dao.Database
import dao.GuerillaProseStorage
import grapgql.AppSchema
import org.koin.dsl.module.applicationContext
import org.koin.dsl.module.module

object DependencyProvider {
    val mainModule = module {
        single { Gson() }
        single { Database() as GuerillaProseStorage }
        single { AppSchema(get()) }
    }
}