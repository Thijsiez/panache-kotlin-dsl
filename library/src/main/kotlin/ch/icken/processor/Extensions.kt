package ch.icken.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.*

internal fun KSDeclaration.isClass(qualifiedClassName: String): Boolean =
    qualifiedName?.asString() == qualifiedClassName

internal fun KSClassDeclaration.isSubclass(qualifiedSuperclassName: String): Boolean =
    getAllSuperTypes().map { it.declaration }
        .any { it.isClass(qualifiedSuperclassName) }

internal fun KSAnnotation.isClass(qualifiedAnnotationName: String): Boolean =
    annotationType.resolve().declaration.isClass(qualifiedAnnotationName)
internal fun KSAnnotation.getArgumentOrNull(argumentName: String): KSValueArgument? =
    arguments.firstOrNull { it.name?.asString() == argumentName }

internal fun KSAnnotated.hasAnnotation(qualifiedAnnotationName: String): Boolean =
    annotations.any { it.isClass(qualifiedAnnotationName) }
internal fun KSAnnotated.getFirstAnnotationOrNull(qualifiedAnnotationName: String): KSAnnotation? =
    annotations.filter { it.isClass(qualifiedAnnotationName) }.firstOrNull()
internal inline fun <reified T : Enum<T>> KSAnnotated.getAnnotationEnumArgumentValue(
    qualifiedAnnotationName: String,
    enumArgumentName: String
): T? =
    getFirstAnnotationOrNull(qualifiedAnnotationName)?.run {
        when (val value = getArgumentOrNull(enumArgumentName)?.value) {
            null -> null
            is T? -> value
            is KSType -> enumValueOf<T>(value.declaration.simpleName.asString())
            else -> null
        }
    }

internal val KSPropertyDeclaration.typeName: String
    get() = type.resolve().declaration.simpleName.asString()
