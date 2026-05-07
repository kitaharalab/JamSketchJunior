package jp.kthrlab.jamsketch.config
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jp.crestmuse.cmx.misc.ChordSymbol2
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

sealed class SealedConfig {

    companion object : AbstractConfig() {
        private const val APP_DATA_PARENT_DIR = "kthrlab"
        private const val APP_DATA_DIR = "jamsketch"
        private val configFileName = APP_DATA_PARENT_DIR
            .plus(File.separator)
            .plus(APP_DATA_DIR)
            .plus(File.separator)
            .plus("config.json")
        private val userJsonFile = File(getAppDataDirectory(), configFileName)
        private val defaultJsonFile = File(javaClass.getResource("/config.json").toURI())
        private val jsonFile = userJsonFile.takeIf { it.exists() } ?: defaultJsonFile
        private val mapper = jacksonObjectMapper().let {
            // Ignore unknown properties
            it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            it
        }
        private val defaultConfig: Config = (mapper.readValue(defaultJsonFile, Config::class.java) as Config)

        @JvmStatic
        protected var config: Config =
            (mapper.readValue(jsonFile, Config::class.java) as Config).let {
                // Merge user's config and default config
                mergeWithDefaultsInPlace(defaultConfig)
                it
            }

        override fun load() {
        }

        @JvmStatic
        override fun save() {
            val parentDir = userJsonFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                makeParentDirs(userJsonFile)
            }
            if (!userJsonFile.exists()) {
                userJsonFile.createNewFile()
            }

            mapper.writeValue(userJsonFile, config)
        }

        /**
         * Merge user's config and default config.
         * In order to identify new setting items, the default setting value is used
         * for the initial value of each type.
         */
        private inline fun <reified T : Any> T.mergeWithDefaultsInPlace(defaults: T): T {
            val clazz = T::class

            clazz.memberProperties.forEach { property ->
                property.isAccessible = true

                val defaultValue = property.get(defaults)
                val userValue = property.get(this)
                val isDefaultValue = when (property.returnType) {
                    String::class.createType() -> userValue == ""
                    Int::class.createType() -> userValue == 0
                    Boolean::class.createType() -> userValue == false
                    Double::class.createType() -> userValue == 0.0
                    Float::class.createType() -> userValue == 0.0f
                    Long::class.createType() -> userValue == 0L
                    Short::class.createType() -> userValue == 0.toShort()
                    Byte::class.createType() -> userValue == 0.toByte()
//                    Array<String>::class.createType() -> userValue == emptyArray<String>()
                    else -> userValue == null
                }

                // Comparison logic:
                // Use default value if user value is null or the same as default value
                if (userValue == null || isDefaultValue) {
                    if (property is KMutableProperty<*>) {
                        property.setter.call(this, defaultValue)
                    }
                }
            }
            return this
        }
    }

}

data object AccessibleConfig : SealedConfig() {

    val config : Config
        get() {
            if (isAssignableFrom(Thread.currentThread().stackTrace)) {
                return SealedConfig.config
            } else {
                throw IllegalAccessException()
            }
        }

    fun save() {
        if (isAssignableFrom(Thread.currentThread().stackTrace)) { SealedConfig.save() }
    }

    private fun isAssignableFrom(stackTrace: Array<StackTraceElement>) : Boolean {
        stackTrace.reversed().forEach {
            val callerClass = Class.forName(it.className)
            if (IConfigAccessible::class.java.isAssignableFrom(callerClass)) return true
        }
        return false
    }
}

data object ModelConfig : SealedConfig() {
//  ModelConfig.config で参照したい場合
//    var config = SealedConfig.config
    fun save()  =  SealedConfig.save()
}

data object ControllerConfig : SealedConfig(

) {

}

data object ViewConfig : SealedConfig() {

}