/*
 * Copyright (C) Lamprism
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

package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import com.lamprism.luxspec.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class ConfigSpecValidationTest {
    @Test
    void rejectsAnInvalidDefaultWhenTheDefinitionIsCreated() {
        Validator<Integer> positive = Validator.of(
                value -> value > 0,
                "Value must be positive"
        );

        assertThrows(
                ConfigValueValidationException.class,
                () -> ConfigSpec.of("sample.limit", ConfigCodec.integer(), 0, false, positive)
        );
    }
}
