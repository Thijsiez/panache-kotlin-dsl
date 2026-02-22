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

package ch.icken.processor

import ch.icken.processor.model.KSClassDeclarationWithProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate

internal class MappedSuperclassProcessor(
    options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : EntityProcessor(options) {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JAKARTA_PERSISTENCE_MAPPED_SUPERCLASS)
            .partition(KSAnnotated::validate)

        valid.filterPanacheEntities()
            .map { it.withColumnProperties() }
            .forEach(::createColumnsBaseClass)

        return invalid
    }

    fun createColumnsBaseClass(entity: KSClassDeclarationWithProperties) {
        //TODO
    }
}

class MappedSuperclassProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        MappedSuperclassProcessor(environment.options, environment.codeGenerator, environment.logger)
}
