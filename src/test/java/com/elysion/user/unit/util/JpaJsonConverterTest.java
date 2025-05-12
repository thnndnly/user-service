// src/test/java/com/elysion/user/util/JpaJsonConverterTest.java
package com.elysion.user.unit.util;

import com.elysion.user.util.JpaJsonConverter;
import org.junit.jupiter.api.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JpaJsonConverterTest {

    private JpaJsonConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JpaJsonConverter();
    }

    @Test
    void convertToDatabaseColumn_nullInput_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_simpleMap_producesJsonString() {
        // Verwende LinkedHashMap, damit Einfügereihenfolge im JSON erhalten bleibt
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("zahl", 42);
        map.put("text", "hallo");
        String json = converter.convertToDatabaseColumn(map);

        // JSON muss genau so aussehen (ohne zusätzliche Whitespaces)
        assertEquals("{\"zahl\":42,\"text\":\"hallo\"}", json);
    }

    @Test
    void convertToEntityAttribute_nullInput_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttribute_validJson_returnsMap() {
        String json = "{\"a\":1,\"b\":\"zwei\",\"flag\":true}";
        Map<String, Object> map = converter.convertToEntityAttribute(json);

        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals("zwei", map.get("b"));
        assertEquals(true, map.get("flag"));
    }

    @Test
    void convertToEntityAttribute_invalidJson_throwsIllegalArgumentException() {
        String badJson = "{nicht: valider json}";
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute(badJson)
        );
        assertTrue(ex.getMessage().contains("Cannot convert JSON to map"));
    }

    @Test
    void roundTrip_mapToJsonAndBack_preservesContent() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("x", 123);
        original.put("y", Map.of("inner", 5));

        String json = converter.convertToDatabaseColumn(original);
        Map<String, Object> restored = converter.convertToEntityAttribute(json);

        assertEquals(original, restored);
    }
}