package ch.icken.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

internal fun KSDeclaration.isClass(qualifiedClassName: String): Boolean =
    qualifiedName?.asString() == qualifiedClassName

internal fun KSClassDeclaration.isSubclass(qualifiedSuperclassName: String): Boolean =
    getAllSuperTypes().map { it.declaration }
        .any { it.isClass(qualifiedSuperclassName) }

internal fun KSPropertyDeclaration.hasAnnotation(qualifiedAnnotationName: String): Boolean =
    annotations.map { it.annotationType.resolve() }
        .map { it.declaration }
        .any { it.isClass(qualifiedAnnotationName) }

internal val KSPropertyDeclaration.typeName: String
    get() = type.resolve().declaration.simpleName.asString()
