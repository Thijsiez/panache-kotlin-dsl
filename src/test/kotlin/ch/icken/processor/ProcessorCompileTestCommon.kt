/*
 * Copyright 2024 Thijs Koppen
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

import ch.icken.query.Column
import com.tschuchort.compiletesting.*
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalCompilerApi::class)
abstract class ProcessorCompileTestCommon {

    protected fun kotlinResource(resourceName: String, sourceFileName: String = resourceName) =
        SourceFile.kotlin(sourceFileName, javaClass.classLoader.getResource(resourceName)?.readText() ?: "")

    protected fun compilation(vararg source: SourceFile) = KotlinCompilation().apply {
        inheritClassPath = true
        kspWithCompilation = true
        sources = listOf(*source)
        symbolProcessorProviders = listOf(
            PanacheCompanionBaseProcessorProvider(),
            PanacheEntityBaseProcessorProvider()
        )
        verbose = false
    }

    protected fun kotlinCompilation(sourceFileName: String, @Language("kotlin") contents: String) =
        compilation(SourceFile.kotlin(sourceFileName, contents))

    protected fun KotlinCompilation.Result.assertOk() = assertEquals(KotlinCompilation.ExitCode.OK, exitCode)

    protected fun KotlinCompilation.assertFileGenerated(fileName: String, outputDir: String = "generated") =
        assertTrue(kspSourcesDir.resolve("kotlin").resolve(outputDir).resolve(fileName).isFile)

    protected fun KotlinCompilation.Result.loadGeneratedClass(className: String, outputPackage: String = "generated") =
        this.classLoader.loadClass("$outputPackage.$className").kotlin

    protected fun KClass<*>.assertNumberOfProperties(expectedNumberOfProperties: Int) {
        assertEquals(expectedNumberOfProperties, memberProperties.size)
    }

    protected fun KClass<*>.assertHasPropertyOfType(propertyName: String, expectedTypeName: String) {
        val property = memberProperties.find { it.name == propertyName }
        assertNotNull(property)

        val returnType = property!!.returnType
        returnType.classifier.let { classifier ->
            assertNotNull(classifier)
            assertTrue(classifier is KClass<*>)
            assertEquals(expectedTypeName, (classifier as? KClass<*>)?.simpleName)
        }
    }

    protected fun KClass<*>.assertHasColumnOfType(columnName: String, expectedType: KClass<*>) {
        val property = memberProperties.find { it.name == columnName }
        assertNotNull(property)

        val returnType = property!!.returnType
        returnType.classifier.let { classifier ->
            assertNotNull(classifier)
            assertTrue(classifier is KClass<*>)
            assertEquals(Column::class, classifier)
        }

        val arguments = returnType.arguments
        assertNotNull(arguments)
        assertEquals(1, arguments.size)

        val type = arguments[0].type
        assertNotNull(type)
        type!!.classifier.let { classifier ->
            assertNotNull(classifier)
            assertTrue(classifier is KClass<*>)
            assertEquals(expectedType, classifier)
        }
    }
}
