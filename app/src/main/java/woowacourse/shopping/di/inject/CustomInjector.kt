package woowacourse.shopping.di.inject

import woowacourse.shopping.di.annotation.CustomInject
import woowacourse.shopping.di.annotation.Qualifier
import woowacourse.shopping.di.container.DependencyContainer
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

class CustomInjector {

    fun <T : Any> inject(kClass: KClass<T>): Any {
        return DependencyContainer.getInstance(kClass) ?: createInstanceFromKClass(kClass)
    }

    private fun <T : Any> createInstanceFromKClass(kClass: KClass<T>): T {
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("주 생성자를 찾을 수 없습니다.")

        val parameterValues =
            constructor.parameters.associateWith { getOrInjectInstanceWithQualifier(it.type.jvmErasure) }

        return constructor.callBy(parameterValues).apply { injectFields(this) }
    }

    private fun <T : Any> injectFields(instance: T) {
        instance::class.declaredMemberProperties
            .filter { it.hasAnnotation<CustomInject>() }
            .forEach { prop ->
                prop.isAccessible = true
                val propertyType = prop.returnType.jvmErasure
                val value = getOrInjectInstanceWithQualifier(propertyType)
                (prop as KMutableProperty<*>).setter.call(instance, value)
            }
    }

    private fun getOrInjectInstanceWithQualifier(kClass: KClass<*>): Any {
        val annotationsWithQualifier = kClass.annotations.filterIsInstance<Qualifier>()
        return DependencyContainer.getInstance(kClass, annotationsWithQualifier) ?: inject(kClass)
    }
}
