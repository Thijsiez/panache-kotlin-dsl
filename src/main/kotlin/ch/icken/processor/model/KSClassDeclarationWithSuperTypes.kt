/*
 * Copyright 2026 Thijs Koppen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.icken.processor.model

import ch.icken.processor.annotation
import ch.icken.processor.hasAnnotation
import ch.icken.processor.isClass
import ch.icken.processor.isParameterSet
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toTypeName

internal class KSClassDeclarationWithSuperTypes(
    ksClassDeclaration: KSClassDeclaration,
    internal val superTypes: Sequence<KSType>
) : KSClassDeclarationWrapper(ksClassDeclaration) {

    internal fun isSubclass(qualifiedSuperclassName: String): Boolean =
        superTypes.any { it.declaration.isClass(qualifiedSuperclassName) }

    internal fun withIdTypeName(): KSClassDeclarationWithIdTypeName? {
        //Determine the type of the @Id column of this class, as specified by the Panache companion object
        val idTypeName = ksClassDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .singleOrNull(KSClassDeclaration::isCompanionObject)
            ?.getAllSuperTypes()
            ?.firstOrNull { it.declaration.isClass(HIBERNATE_PANACHE_COMPANION_BASE) }
            ?.arguments
            //The @Id column type argument is the last one
            ?.lastOrNull()
            ?.toTypeName()
            ?: return null

        return KSClassDeclarationWithIdTypeName(ksClassDeclaration, idTypeName)
    }

    internal fun withColumnProperties(): KSClassDeclarationWithProperties {
        val columnProperties = ksClassDeclaration.getDeclaredProperties()
            //When a property has no backing field, there's nothing to save, so we won't generate a column
            .filter { it.hasBackingField }
            //When a property is annotated with JPA's @Transient, it is not mapped to a column in the database
            // Therefore, this property can never be used in a query, so we won't generate a Column for it
            .filterNot { it.hasAnnotation(JAKARTA_PERSISTENCE_TRANSIENT) }
            //When a property is mapped by another entity, we'd have to use a JOIN to use it in a query
            // Currently, a JOIN is not supported syntax-wise, so we won't generate Columns for these properties
            .filterNot { it.annotation(JAKARTA_PERSISTENCE_MANY_TO_MANY).isParameterSet(PARAM_NAME_MAPPED_BY) }
            .filterNot { it.annotation(JAKARTA_PERSISTENCE_ONE_TO_MANY).isParameterSet(PARAM_NAME_MAPPED_BY) }
            .filterNot { it.annotation(JAKARTA_PERSISTENCE_ONE_TO_ONE).isParameterSet(PARAM_NAME_MAPPED_BY) }
            .toList()

        return KSClassDeclarationWithProperties(ksClassDeclaration, columnProperties)
    }

    internal companion object {
        //region Constants
        internal const val PARAM_NAME_MAPPED_BY = "mappedBy"
        //endregion
        //region Names
        internal const val HIBERNATE_PANACHE_COMPANION_BASE: String =
            "io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase"
        private const val JAKARTA_PERSISTENCE_MANY_TO_MANY: String = "jakarta.persistence.ManyToMany"
        private const val JAKARTA_PERSISTENCE_ONE_TO_MANY: String = "jakarta.persistence.OneToMany"
        private const val JAKARTA_PERSISTENCE_ONE_TO_ONE: String = "jakarta.persistence.OneToOne"
        internal const val JAKARTA_PERSISTENCE_TRANSIENT: String = "jakarta.persistence.Transient"
        //endregion
    }
}

internal fun KSClassDeclaration.withSuperTypes() =
    KSClassDeclarationWithSuperTypes(this, getAllSuperTypes())
