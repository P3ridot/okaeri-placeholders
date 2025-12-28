package eu.okaeri.placeholderstest;

import eu.okaeri.placeholders.Placeholders;
import eu.okaeri.placeholders.context.PlaceholderContext;
import eu.okaeri.placeholderstest.schema.external.ExternalItem;
import eu.okaeri.placeholderstest.schema.external.ExternalMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestPlaceholderValueExtraction {

    @Test
    public void test_extract_simple_value() {
        PlaceholderContext context = PlaceholderContext.create()
            .with("name", "John")
            .with("age", 25);

        String name = context.getPlaceholderValue("name", String.class);
        assertEquals("John", name);

        Integer age = context.getPlaceholderValue("age", Integer.class);
        assertEquals(25, age);
    }

    @Test
    public void test_extract_nested_value() {
        Placeholders placeholders = Placeholders.create()
            .registerPlaceholder(ExternalItem.class, "type", (e, a, o) -> e.getType())
            .registerPlaceholder(ExternalItem.class, "amount", (e, a, o) -> e.getAmount())
            .registerPlaceholder(ExternalItem.class, "meta", (e, a, o) -> e.getMeta())
            .registerPlaceholder(ExternalMeta.class, "name", (e, a, o) -> e.getName())
            .registerPlaceholder(ExternalMeta.class, "lore", (e, a, o) -> e.getLore());

        ExternalItem item = new ExternalItem();
        item.setAmount(123);
        item.setType("Stone");
        ExternalMeta meta = new ExternalMeta();
        meta.setName("Red stone");
        meta.setLore("Really nice stone. I like it.");
        item.setMeta(meta);

        PlaceholderContext context = PlaceholderContext.create()
            .setPlaceholders(placeholders)
            .with("item", item);

        // Extract nested value
        String metaName = context.getPlaceholderValue("item.meta.name", String.class);
        assertEquals("Red stone", metaName);

        // Extract intermediate object
        ExternalMeta extractedMeta = context.getPlaceholderValue("item.meta", ExternalMeta.class);
        assertEquals(meta, extractedMeta);
        assertEquals("Red stone", extractedMeta.getName());

        // Extract primitive type
        Integer amount = context.getPlaceholderValue("item.amount", Integer.class);
        assertEquals(123, amount);
    }

    @Test
    public void test_extract_with_type_mismatch() {
        PlaceholderContext context = PlaceholderContext.create()
            .with("age", 25);

        // Try to extract as wrong type
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            context.getPlaceholderValue("age", String.class);
        });

        assertTrue(exception.getMessage().contains("returned type"));
        assertTrue(exception.getMessage().contains("Integer"));
        assertTrue(exception.getMessage().contains("String"));
    }

    @Test
    public void test_extract_missing_placeholder() {
        PlaceholderContext context = PlaceholderContext.create()
            .with("name", "John");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            context.getPlaceholderValue("missing", String.class);
        });

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    public void test_extract_with_mapper() {
        PlaceholderContext context = PlaceholderContext.create()
            .with("age", 25);

        // Convert Integer to String using mapper
        String ageStr = context.getPlaceholderValue("age", obj -> String.valueOf(obj), String.class);
        assertEquals("25", ageStr);

        // Convert Integer to Double using mapper
        Double ageDouble = context.getPlaceholderValue("age", obj -> ((Integer) obj).doubleValue(), Double.class);
        assertEquals(25.0, ageDouble);
    }

    @Test
    public void test_extract_nested_with_mapper() {
        Placeholders placeholders = Placeholders.create()
            .registerPlaceholder(ExternalItem.class, "amount", (e, a, o) -> e.getAmount());

        ExternalItem item = new ExternalItem();
        item.setAmount(123);

        PlaceholderContext context = PlaceholderContext.create()
            .setPlaceholders(placeholders)
            .with("item", item);

        // Extract and convert amount to string
        String amountStr = context.getPlaceholderValue("item.amount", obj -> "Amount: " + obj, String.class);
        assertEquals("Amount: 123", amountStr);
    }

    @Test
    public void test_extract_null_value() {
        Placeholders placeholders = Placeholders.create()
            .registerPlaceholder(ExternalItem.class, "meta", (e, a, o) -> e.getMeta());

        ExternalItem item = new ExternalItem();
        item.setMeta(null);

        PlaceholderContext context = PlaceholderContext.create()
            .setPlaceholders(placeholders)
            .with("item", item);

        // Extracting null should throw exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            context.getPlaceholderValue("item.meta", ExternalMeta.class);
        });

        assertTrue(exception.getMessage().contains("resolved to null"));
    }

    @Test
    public void test_extract_chained_values() {
        Placeholders placeholders = Placeholders.create()
            .registerPlaceholder(ExternalItem.class, "amount", (e, a, o) -> e.getAmount())
            .registerPlaceholder(ExternalItem.class, "meta", (e, a, o) -> e.getMeta())
            .registerPlaceholder(ExternalMeta.class, "name", (e, a, o) -> e.getName());

        ExternalItem item = new ExternalItem();
        item.setAmount(456);
        ExternalMeta meta = new ExternalMeta();
        meta.setName("Special Item");
        item.setMeta(meta);

        PlaceholderContext context = PlaceholderContext.create()
            .setPlaceholders(placeholders)
            .with("item", item);

        // Extract multiple values
        Integer amount = context.getPlaceholderValue("item.amount", Integer.class);
        assertEquals(456, amount);

        String name = context.getPlaceholderValue("item.meta.name", String.class);
        assertEquals("Special Item", name);
    }
}
