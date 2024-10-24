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

import ch.icken.processor.ClassNames.SuppressClassName
import com.squareup.kotlinpoet.AnnotationSpec

internal object GenerationValues {
    const val GENERATED_PACKAGE_SUFFIX = ".generated"
    const val COLUMN_NAME_OBJECT_SUFFIX = "Columns"
    const val COLUMN_NAME_BASE_CLASS_SUFFIX = "Base"
    const val COLUMN_NAME_BASE_CLASS_PARAM_NAME = "parent"

    const val EXTENSIONS_FILE = "PanacheCompanionBaseExtensions"
    const val EXPRESSION_PARAM_NAME = "expression"
    const val GROUP_COMPONENT_PARAM_NAME = "groupComponent"
    const val SORT_PARAM_NAME = "sort"

    const val WHERE = "where"
    const val AND = "and"
    const val OR = "or"

    private const val GROUP = "Group"
    const val WHERE_GROUP = "$WHERE$GROUP"
    const val AND_GROUP = "$AND$GROUP"
    const val OR_GROUP = "$OR$GROUP"

    const val COUNT = "count"
    const val DELETE = "delete"
    const val FIND = "find"
    const val FIND_SORTED = "findSorted"
    const val STREAM = "stream"
    const val STREAM_SORTED = "streamSorted"

    const val SINGLE = "single"
    const val SINGLE_SAFE = "singleSafe"
    const val MULTIPLE = "multiple"
    const val MULTIPLE_SORTED = "multipleSorted"

    val FileSuppress = AnnotationSpec.builder(SuppressClassName)
        .addMember("%S", "RedundantVisibilityModifier")
        .addMember("%S", "unused")
        .build()
}
