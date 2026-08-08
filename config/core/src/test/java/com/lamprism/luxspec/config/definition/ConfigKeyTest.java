package com.lamprism.luxspec.config.definition;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigParameter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigKeyTest {
    @Test
    void usesOneModelForFixedAndParameterizedKeys() {
        ConfigKey fixed = ConfigKey.of("application.timeout");
        ConfigKey expression = ConfigKey.template(
                "service.{tenant}.timeout",
                List.of(new ConfigParameter("tenant", Set.of("north")))
        );

        assertFalse(fixed.isParameterized());
        assertTrue(expression.isParameterized());
        assertEquals(List.of("application", "timeout"), fixed.getSegments());
        assertEquals(List.of("service", "{tenant}", "timeout"), expression.getSegments());
    }

    @Test
    void bindsAndMatchesThroughTheUnifiedKeyType() {
        ConfigKey expression = ConfigKey.template(
                "service.{tenant}.timeout",
                List.of(new ConfigParameter("tenant", Set.of("north")))
        );

        ConfigKey complete = expression.bind(Map.of("tenant", "north"));

        assertFalse(complete.isParameterized());
        assertEquals("service.north.timeout", complete.getValue());
        assertEquals(Map.of("tenant", "north"), expression.match(complete));
    }
}
