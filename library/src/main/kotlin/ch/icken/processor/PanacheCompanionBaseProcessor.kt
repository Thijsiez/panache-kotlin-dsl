/*
 * Copyright 2023 Thijs Koppen
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

import ch.icken.processor.ClassNames.AndQueryComponentClassName
import ch.icken.processor.ClassNames.BooleanExpressionClassName
import ch.icken.processor.ClassNames.JvmNameClassName
import ch.icken.processor.ClassNames.OrQueryComponentClassName
import ch.icken.processor.ClassNames.QueryComponentClassName
import ch.icken.processor.GenerationOptions.ADD_GENERATED_ANNOTATION
import ch.icken.processor.GenerationOptions.generatedAnnotation
import ch.icken.processor.GenerationValues.AND
import ch.icken.processor.GenerationValues.AND_GROUP
import ch.icken.processor.GenerationValues.COLUMN_NAME_OBJECT_SUFFIX
import ch.icken.processor.GenerationValues.COMPANION_OBJECT
import ch.icken.processor.GenerationValues.EXPRESSION_PARAM_NAME
import ch.icken.processor.GenerationValues.EXTENSIONS_FILE
import ch.icken.processor.GenerationValues.FileSuppress
import ch.icken.processor.GenerationValues.GENERATED_PACKAGE_SUFFIX
import ch.icken.processor.GenerationValues.GROUP_COMPONENT_PARAM_NAME
import ch.icken.processor.GenerationValues.OR
import ch.icken.processor.GenerationValues.OR_GROUP
import ch.icken.processor.GenerationValues.WHERE
import ch.icken.processor.GenerationValues.WHERE_GROUP
import ch.icken.processor.QualifiedNames.HibernatePanacheCompanionBase
import ch.icken.processor.QualifiedNames.HibernatePanacheEntityBase
import ch.icken.processor.QualifiedNames.JakartaPersistenceEntity
import ch.icken.processor.QualifiedNames.JakartaPersistenceId
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class PanacheCompanionBaseProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(JakartaPersistenceEntity)
            .partition(KSAnnotated::validate)

        val addGeneratedAnnotation = options[ADD_GENERATED_ANNOTATION].toBoolean()

        valid.filterIsInstance<KSClassDeclaration>()
            .filter { it.isSubclass(HibernatePanacheEntityBase) }
            .filter { ksClassDeclaration ->
                ksClassDeclaration.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .filter(KSClassDeclaration::isCompanionObject)
                    .filter { it.isSubclass(HibernatePanacheCompanionBase) }
                    .any()
            }
            .groupBy { it.packageName.asString() }
            .forEach { (packageName, ksClasses) ->
                createQueryBuilderExtensions(packageName, ksClasses, addGeneratedAnnotation)
            }

        return invalid
    }

    private fun createQueryBuilderExtensions(originalPackageName: String, ksClasses: List<KSClassDeclaration>,
                                             addGeneratedAnnotation: Boolean) {
        val packageName = originalPackageName + GENERATED_PACKAGE_SUFFIX
        logger.info("Generating $packageName.$EXTENSIONS_FILE")

        val funSpecs = ksClasses.flatMap { ksClass ->
            val className = ksClass.toClassName()
            val objectName = ksClass.simpleName.asString() + COLUMN_NAME_OBJECT_SUFFIX
            val objectClassName = ClassName(packageName, objectName)

            val idClassName = ksClass.getAllProperties()
                .filter { it.hasAnnotation(JakartaPersistenceId) }
                .single()
                .type.resolve()
                .toClassName()

            val expressionParameterType = LambdaTypeName.get(
                receiver = objectClassName,
                returnType = BooleanExpressionClassName
            )
            val queryComponentType = QueryComponentClassName
                .plusParameter(className)
                .plusParameter(idClassName)
            val groupComponentParameterType = LambdaTypeName.get(
                receiver = queryComponentType,
                returnType = queryComponentType
            )

            val whereReceiver = className.nestedClass(COMPANION_OBJECT)
            val where = FunSpec.builder(WHERE)
                .addModifiers(KModifier.INLINE)
                .receiver(whereReceiver)
                .addParameter(EXPRESSION_PARAM_NAME, expressionParameterType)
                .returns(queryComponentType)
                .addStatement("return %M($EXPRESSION_PARAM_NAME(%T))",
                    MemberName(QueryComponentClassName.packageName, WHERE), objectClassName)
                .addAnnotation(jvmNameAnnotation("$WHERE$objectName"))
            val whereGroup = FunSpec.builder(WHERE_GROUP)
                .receiver(whereReceiver)
                .addParameter(EXPRESSION_PARAM_NAME, expressionParameterType)
                .addParameter(GROUP_COMPONENT_PARAM_NAME, groupComponentParameterType)
                .returns(queryComponentType)
                .addStatement("return %M($EXPRESSION_PARAM_NAME(%T), $GROUP_COMPONENT_PARAM_NAME)",
                    MemberName(QueryComponentClassName.packageName, WHERE_GROUP), objectClassName)
                .addAnnotation(jvmNameAnnotation("$WHERE_GROUP$objectName"))

            val and = FunSpec.builder(AND)
                .addModifiers(KModifier.INLINE)
                .receiver(queryComponentType)
                .addParameter(EXPRESSION_PARAM_NAME, expressionParameterType)
                .returns(AndQueryComponentClassName.plusParameter(className).plusParameter(idClassName))
                .addStatement("return $AND($EXPRESSION_PARAM_NAME(%T))", objectClassName)
                .addAnnotation(jvmNameAnnotation("$AND$objectName"))
            val andGroup = FunSpec.builder(AND_GROUP)
                .receiver(queryComponentType)
                .addParameter(EXPRESSION_PARAM_NAME, expressionParameterType)
                .addParameter(GROUP_COMPONENT_PARAM_NAME, groupComponentParameterType)
                .returns(queryComponentType)
                .addStatement("return $AND_GROUP($EXPRESSION_PARAM_NAME(%T), $GROUP_COMPONENT_PARAM_NAME)",
                    objectClassName)
                .addAnnotation(jvmNameAnnotation("$AND_GROUP$objectName"))

            val or = FunSpec.builder(OR)
                .addModifiers(KModifier.INLINE)
                .receiver(queryComponentType)
                .addParameter(EXPRESSION_PARAM_NAME, expressionParameterType)
                .returns(OrQueryComponentClassName.plusParameter(className).plusParameter(idClassName))
                .addStatement("return $OR($EXPRESSION_PARAM_NAME(%T))", objectClassName)
                .addAnnotation(jvmNameAnnotation("$OR$objectName"))
            val orGroup = FunSpec.builder(OR_GROUP)
                .receiver(queryComponentType)
                .addParameter(EXPRESSION_PARAM_NAME, expressionParameterType)
                .addParameter(GROUP_COMPONENT_PARAM_NAME, groupComponentParameterType)
                .returns(queryComponentType)
                .addStatement("return $OR_GROUP($EXPRESSION_PARAM_NAME(%T), $GROUP_COMPONENT_PARAM_NAME)",
                    objectClassName)
                .addAnnotation(jvmNameAnnotation("$OR_GROUP$objectName"))

            listOf(where, whereGroup, and, andGroup, or, orGroup)
        }

        FileSpec.builder(packageName, EXTENSIONS_FILE)
            .apply {
                funSpecs.onEach { it.addAnnotationIf(GeneratedAnnotation, addGeneratedAnnotation) }
                    .map(FunSpec.Builder::build)
                    .forEach(::addFunction)
            }
            .addAnnotation(FileSuppress)
            .addAnnotationIf(GeneratedAnnotation, addGeneratedAnnotation)
            .build()
            .writeTo(codeGenerator, Dependencies(false))
    }

    private fun jvmNameAnnotation(name: String) =
        AnnotationSpec.builder(JvmNameClassName)
            .addMember("%S", name)
            .build()

    companion object {
        private val GeneratedAnnotation = generatedAnnotation(PanacheCompanionBaseProcessor::class.java)
    }
}

class PanacheCompanionBaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PanacheCompanionBaseProcessor(environment.options, environment.codeGenerator, environment.logger)
}
