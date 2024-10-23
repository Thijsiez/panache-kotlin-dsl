/*
 * Copyright 2023-2024 Thijs Koppen
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

import ch.icken.processor.ClassNames.GeneratedClassName
import com.squareup.kotlinpoet.AnnotationSpec
import java.time.LocalDateTime

object GenerationOptions {
    const val ADD_GENERATED_ANNOTATION = "addGeneratedAnnotation"
    fun <T : Any> generatedAnnotation(
        generator: Class<T>,
        date: LocalDateTime = LocalDateTime.now(),
        comments: String = "Generated using panache-kotlin-dsl"
    ) = AnnotationSpec.builder(GeneratedClassName)
        .addMember("%S", generator.name)
        .addMember("%S", date.toString())
        .addMember("%S", comments)
        .build()
}
