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

package ch.icken.model

import ch.icken.*
import ch.icken.query.PanacheSingleResult
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class ExamplesTest {

    @Test
    fun runAllExamples() {

        // Given

        // When
        val john = findJohn()
        val numberOfNotMen = countNotMen()
        val bornBeforeEpoch = bornBefore(LocalDate.EPOCH)
        val sons = findAllSons()
        val averageSalary = averageOfVerySpecificSalaryRanges()

        // Then
        assertInstanceOf(PanacheSingleResult.Result::class.java, john)
        assertEquals(4, numberOfNotMen)
        assertEquals(2, bornBeforeEpoch.size)
        assertEquals(2, sons.size)
        assertEquals(67_500.0, averageSalary)
    }
}
