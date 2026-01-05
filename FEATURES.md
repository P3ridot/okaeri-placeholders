# Okaeri Placeholders - Complete Feature Summary

This document provides a comprehensive summary of all features in the okaeri-placeholders library, primarily based on the test suite and documentation. This is intended to help developers understand the full capabilities when creating similar libraries.

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [Placeholder Syntax](#placeholder-syntax)
3. [Message Compilation](#message-compilation)
4. [Placeholder Context](#placeholder-context)
5. [Metadata Features](#metadata-features)
6. [Duration Formatting](#duration-formatting)
7. [Instant/Time Formatting](#instanttime-formatting)
8. [Nested Placeholders & Subfields](#nested-placeholders--subfields)
9. [Method Parameters](#method-parameters)
10. [Value Extraction API](#value-extraction-api)
11. [Custom Placeholder Registration](#custom-placeholder-registration)
12. [Inheritance Support](#inheritance-support)
13. [Reflection-Based Placeholders](#reflection-based-placeholders)
14. [Built-in String Transformations](#built-in-string-transformations)
15. [Platform-Specific Integrations](#platform-specific-integrations)
16. [Performance Characteristics](#performance-characteristics)

---

## Core Concepts

### 1. Compiled Messages

Messages are compiled once and can be reused multiple times with different placeholder values. This is the key to the library's performance.

**Examples from tests:**
```java
// Compile once, reuse many times
CompiledMessage message = CompiledMessage.of("Hello {who}! How are you {when}?");

// Empty message
CompiledMessage empty = CompiledMessage.of(""); // No parts

// Static message (no placeholders)
CompiledMessage static = CompiledMessage.of("Hello World!"); // Single MessageStatic part

// Message with placeholders
CompiledMessage withFields = CompiledMessage.of("Hello {who}!");
// Parts: [MessageStatic("Hello "), MessageField("who"), MessageStatic("!")]
```

### 2. Placeholder Context

Context holds placeholder values and applies them to compiled messages.

**Two modes:**
- **Reusable context**: `PlaceholderContext.create()` + `.apply(message)` - for applying same placeholders to multiple messages
- **One-time context**: `PlaceholderContext.of(message)` + `.apply()` - optimized for single use (faster when using only partial placeholders)

**Examples from tests:**
```java
// One-time context (recommended for most use cases)
PlaceholderContext context = PlaceholderContext.of(message)
    .with("who", "World")
    .with("when", "today")
    .with("how", "ok");
String result = context.apply(); // "Hello World! How are you today? I'm ok."

// Reusable context
PlaceholderContext reusableContext = PlaceholderContext.create()
    .with("name", "John");
String msg1 = reusableContext.apply(message1);
String msg2 = reusableContext.apply(message2);
```

---

## Placeholder Syntax

### Basic Format

```
{placeholder_name}
```

### With Fallback Value

```
{placeholder_name|fallback_value}
```

If the placeholder is null or not found, the fallback value is used.

**Example from tests:**
```java
// With null values
"{item.meta.name|i didn't}" // Returns "i didn't" when meta.name is null
"{item.meta.lore|no}" // Returns "no" when meta.lore is null
```

### With Subfields (Nested Access)

```
{object.property}
{object.subobject.property}
```

**Example from tests:**
```java
"{player.name}"
"{player.inventory.size}"
"{item.meta.name}"
"{player.itemInHand.itemMeta.displayName}"
```

### With Method Parameters

```
{object.method(param1,param2)}
{object.subobject.method(a,b,c)}
```

**Example from tests:**
```java
"{player.healthBar(20)}"
"{player.healthBar(20,X)}"
"{player.papi(okapibridge_player.name)}"
"{player.stats.value(kills)}"
```

---

## Message Compilation

### Message Parts

Compiled messages consist of two types of parts:
- **MessageStatic**: Static text that doesn't change
- **MessageField**: Placeholder fields that need values

**Example from tests:**
```java
CompiledMessage message = CompiledMessage.of("Hello {who}! How are you {when}? I'm {how}.");

// message.getParts() contains:
// [
//   MessageStatic("Hello "),
//   MessageField("who"),
//   MessageStatic("! How are you "),
//   MessageField("when"),
//   MessageStatic("? I'm "),
//   MessageField("how"),
//   MessageStatic(".")
// ]
```

### Message Fields

MessageField objects can have:
- **Name**: The field identifier
- **Sub**: Nested field for property access
- **Params**: Method parameters
- **Metadata**: For formatting (pluralization, etc.)
- **Fallback**: Default value if field is null

**Example from tests:**
```java
MessageField field = MessageField.of("player.healthBar(20,X)");
// field.getName() = "player"
// field.getSub().getName() = "healthBar"
// field.getSub().getParams().strArr() = ["20", "X"]
```

---

## Metadata Features

### 1. Pluralization

Supports 143 locales via okaeri-pluralize integration.

**Format:**
```
{singular,plural#reference_field}
```

**Examples from tests:**
```java
// English (2 forms)
CompiledMessage msg = CompiledMessage.of(Locale.ENGLISH, 
    "I would like {amount} {apple,apples#amount}.");
// amount=-1: "I would like -1 apples."
// amount=0:  "I would like 0 apples."
// amount=1:  "I would like 1 apple."
// amount=2:  "I would like 2 apples."

// Polish (3 forms)
CompiledMessage msg = CompiledMessage.of(Locale.forLanguageTag("pl"),
    "Mam w domu {dogs} {psa,psy,psów#dogs}.");
// dogs=1:  "Mam w domu 1 psa."
// dogs=2:  "Mam w domu 2 psy."
// dogs=5:  "Mam w domu 5 psów."
// dogs=22: "Mam w domu 22 psy."
// dogs=25: "Mam w domu 25 psów."
```

### 2. Boolean Translation

**Format:**
```
{true_text,false_text#boolean_field}
```

**Example from tests:**
```java
CompiledMessage message = CompiledMessage.of("Active: {yes,no#status}");
// status=true:  "Active: yes"
// status=false: "Active: no"
```

### 3. Number Formatting

**Format:**
```
{%.Nf#number_field}
```
Where N is the number of decimal places.

**Examples from tests:**
```java
// 2 decimal places
CompiledMessage msg = CompiledMessage.of("Value: {%.2f#value}");
// value=0.2: "Value: 0.20"
// value=1:   "Value: 1.00"

// 0 decimal places (rounding)
CompiledMessage msg = CompiledMessage.of("Value: {%.0f#value}");
// value=0.2: "Value: 0"
// value=0.6: "Value: 1"
// value=1:   "Value: 1"
```

---

## Duration Formatting

Comprehensive duration formatting with multiple precision levels.

### Basic Duration Format

**Format:**
```
{duration_field}           // Default: seconds precision
{duration_field(precision)}  // With custom precision
```

**Precision levels:** `ns`, `ms`, `s`, `m`, `h`, `d`

**Examples from tests:**
```java
Duration dur = Duration.ofDays(88)
    .plus(Duration.ofHours(21))
    .plus(Duration.ofMinutes(37))
    .plus(Duration.ofSeconds(4))
    .plus(Duration.ofMillis(200));

// Different precision levels
"{d}":      "88d21h37m4s"      // Default (seconds)
"{d(ms)}":  "88d21h37m4s200ms" // Milliseconds
"{d(m)}":   "88d21h37m"        // Minutes
"{d(h)}":   "88d21h"           // Hours
"{d(d)}":   "88d"              // Days

// Negative durations
Duration.ofDays(-1).minus(Duration.ofHours(12))
"{d}": "-1d12h"
```

### Component Access

Access individual duration components:

**Examples from tests:**
```java
// Individual components
"{d.days}"    // Just the days component
"{d.hours}"   // Just the hours component (0-23)
"{d.minutes}" // Just the minutes component (0-59)
"{d.seconds}" // Just the seconds component (0-59)
"{d.millis}"  // Just the milliseconds component (0-999)

// With pluralization
"{d.days} {day,days#d.days}"
// Duration.ofDays(1): "1 day"
// Duration.ofDays(2): "2 days"
```

### Custom Duration Format

**Format:**
```
{duration_field.format(pattern)}
```

**Pattern syntax:**
- `[unit]` - Required component (always shown)
- `(unit)` - Optional component (only if non-zero)
- `<text,texts>` - Pluralization for the unit

**Examples from tests:**
```java
// Simple format
"{d.format((m)< minute, minutes>)}"
Duration.ofMinutes(5): "5 minutes"

// Complex format with required and optional components
"{d.format([h]< hour, hours> (m)< minute, minutes>)}"
Duration.ofHours(1).plusMinutes(5): "1 hour 5 minutes"
Duration.ofHours(2).plusMinutes(1): "2 hours 1 minute"

// Compact format
"{d.format([d]d(h)h)}"
Duration.ofDays(1).plusHours(2): "1d2h"

// With separators
"{d.format([d]d (h)h)}"
Duration.ofDays(1).plusHours(2): "1d 2h"

"{d.format([d] d, (h) h)}"
Duration.ofDays(1).plusHours(2): "1 d, 2 h"
```

### Zero Duration Handling

**Examples from tests:**
```java
Duration.ZERO:
"{d(d)}":  "0d"
"{d(h)}":  "0h"
"{d(m)}":  "0m"
"{d(s)}":  "0s"
"{d(ms)}": "0ms"
"{d(ns)}": "0ns"
```

### Too-Short Duration Handling

If duration is shorter than requested precision, shows in appropriate unit:

**Examples from tests:**
```java
Duration.ofMinutes(5):
"{d(h)}": "5m"  // Less than 1 hour, shows minutes

Duration.ofSeconds(5):
"{d(h)}": "5s"  // Less than 1 hour, shows seconds

Duration.ofMillis(23):
"{d(s)}": "23ms" // Less than 1 second, shows milliseconds
```

---

## Instant/Time Formatting

Localized date/time formatting with timezone support.

### Formats

**Localized Time (`lt`):**
```
{lt,style,timezone#instant_field}
```

**Localized DateTime (`ldt`):**
```
{ldt,style,timezone#instant_field}
```

**Localized Date (`ld`):**
```
{ld,style,timezone#instant_field}
```

**Custom Pattern (`p`):**
```
{p,pattern,timezone#instant_field}
```

**Styles:** `short`, `medium`, `long`

### Examples from Tests

```java
Instant epoch = Instant.ofEpochSecond(0);

// Localized Time (English, Paris timezone)
"{lt,short,Europe/Paris#time}":  "1:00 AM"
"{lt,medium,Europe/Paris#time}": "1:00:00 AM"
"{lt,long,Europe/Paris#time}":   "1:00:00 AM CET"

// Localized DateTime (Polish, Warsaw timezone)
"{ldt,short,Europe/Warsaw#time}":  "01.01.1970, 01:00"
"{ldt,medium,Europe/Warsaw#time}": "1 sty 1970, 01:00:00"
"{ldt,long,Europe/Warsaw#time}":   "1 stycznia 1970 01:00:00 CET"

// Localized Date (Japanese, Tokyo timezone)
"{ld,short,Asia/Tokyo#time}":  "1970/01/01"
"{ld,medium,Asia/Tokyo#time}": "1970/01/01"
"{ld,long,Asia/Tokyo#time}":   "1970年1月1日"

// Custom pattern (with escaping for commas)
"{p,yyyy/MM/dd\\, HH:mm G,Europe/Paris#time}"
// English: "1970/01/01, 01:00 AD"
// Polish:  "1970/01/01, 01:00 n.e."
// Japanese: "1970/01/01, 01:00 西暦"
```

---

## Nested Placeholders & Subfields

### Single Nesting

**Examples from tests:**
```java
MessageField field = MessageField.of("player.name");
// field.getName() = "player"
// field.getSub().getName() = "name"
```

### Double Nesting

**Examples from tests:**
```java
MessageField field = MessageField.of("player.inventory.name");
// field.getName() = "player"
// field.getSub().getName() = "inventory"
// field.getSub().getSub().getName() = "name"
```

### Multiple Levels

**Examples from tests:**
```java
MessageField field = MessageField.of("player.itemInHand.itemMeta.displayName");
// Depth: 4 levels
// player -> itemInHand -> itemMeta -> displayName
```

### Usage in Messages

**Examples from tests:**
```java
Item item = new Item();
item.setType("Stone");
item.setAmount(123);
Meta meta = new Meta();
meta.setName("Red stone");
item.setMeta(meta);

CompiledMessage message = CompiledMessage.of(
    "Look at my {item.type} x {item.amount}! " +
    "I named it '{item.meta.name}'"
);

PlaceholderContext.of(message).with("item", item).apply();
// Result: "Look at my Stone x 123! I named it 'Red stone'"
```

---

## Method Parameters

### Basic Parameters

**Examples from tests:**
```java
// Single parameter
"{player.healthBar(20)}"
// Params: ["20"]

// Multiple parameters
"{player.healthBar(20,X)}"
// Params: ["20", "X"]
```

### Empty Parameters

**Examples from tests:**
```java
// Empty parameter list
"{player.kill()}"
// Params: [""]
```

### Special Characters in Parameters

**Examples from tests:**
```java
// Parameters with special characters
"{player.healthBar((,20,X,|)}"
// Params: ["(", "20", "X", "|"]

// Escaped characters
"{player.healthBar((,20,,|,)\\, )}"
// Params: ["(", "20", "", "|", "), "]
```

### Nested Parameters

**Examples from tests:**
```java
// Nested placeholder references in parameters
"{player.papi(okapibridge_player.name)}"
// Params: ["okapibridge_player.name"]

// Deeply nested
"{player.papi(okapibridge_player.papi(okapibridge_player.name))}"
// Params: ["okapibridge_player.papi(okapibridge_player.name)"]
```

### Parameters with Multiple Methods

**Examples from tests:**
```java
// Chain of methods with parameters
"{a.b.c(1,2).f.g(33(),.4,.()5)}"
// First method c: params ["1", "2"]
// Second method g: params ["33()", ".4", ".()5"]
```

### Combined with Metadata and Fallbacks

**Examples from tests:**
```java
// Parameters with metadata and fallback
"{some,trash#player.healthBar(20,X)|def}"
// Method: healthBar
// Params: ["20", "X"]
// Metadata: "some,trash"
// Fallback: "def"
```

---

## Value Extraction API

Programmatic access to placeholder values without rendering to string.

### Simple Value Extraction

**Examples from tests:**
```java
PlaceholderContext context = PlaceholderContext.create()
    .with("name", "John")
    .with("age", 25);

// Extract string value
Optional<String> name = context.getPlaceholderValue("name", String.class);
// name.get() = "John"

// Extract integer value
Optional<Integer> age = context.getPlaceholderValue("age", Integer.class);
// age.get() = 25
```

### Nested Value Extraction

**Examples from tests:**
```java
ExternalItem item = new ExternalItem();
item.setAmount(123);
ExternalMeta meta = new ExternalMeta();
meta.setName("Red stone");
item.setMeta(meta);

PlaceholderContext context = PlaceholderContext.create()
    .setPlaceholders(placeholders)
    .with("item", item);

// Extract nested value
Optional<String> metaName = context.getPlaceholderValue("item.meta.name", String.class);
// metaName.get() = "Red stone"

// Extract intermediate object
Optional<ExternalMeta> extractedMeta = context.getPlaceholderValue("item.meta", ExternalMeta.class);
// extractedMeta.get() = meta object
```

### Type Safety

**Examples from tests:**
```java
PlaceholderContext context = PlaceholderContext.create()
    .with("age", 25);

// Wrong type returns empty Optional
Optional<String> result = context.getPlaceholderValue("age", String.class);
// result.isPresent() = false

// Missing placeholder throws exception
context.getPlaceholderValue("missing", String.class);
// Throws: IllegalArgumentException
```

### With Value Mapper

Transform values during extraction:

**Examples from tests:**
```java
PlaceholderContext context = PlaceholderContext.create()
    .with("age", 25);

// Convert Integer to String
Optional<String> ageStr = context.getPlaceholderValue(
    "age", 
    obj -> String.valueOf(obj), 
    String.class
);
// ageStr.get() = "25"

// Convert Integer to Double
Optional<Double> ageDouble = context.getPlaceholderValue(
    "age",
    obj -> ((Integer) obj).doubleValue(),
    Double.class
);
// ageDouble.get() = 25.0

// Transform nested value
Optional<String> amountStr = context.getPlaceholderValue(
    "item.amount",
    obj -> "Amount: " + obj,
    String.class
);
// amountStr.get() = "Amount: 123"
```

### With Function Parameters

**Examples from tests:**
```java
class Stats {
    public String getStatValue(String statName) {
        if ("kills".equals(statName)) return "150";
        if ("deaths".equals(statName)) return "42";
        return "unknown";
    }
}

Placeholders placeholders = Placeholders.create()
    .registerPlaceholder(Stats.class, "value", (stats, field, ctx) -> {
        String statName = field.params().strAt(0, "unknown");
        return stats.getStatValue(statName);
    });

PlaceholderContext context = PlaceholderContext.create()
    .setPlaceholders(placeholders)
    .with("stats", new Stats());

// Extract with function parameter
Optional<String> kills = context.getPlaceholderValue("stats.value(kills)", String.class);
// kills.get() = "150"

Optional<String> deaths = context.getPlaceholderValue("stats.value(deaths)", String.class);
// deaths.get() = "42"
```

### Null Handling

**Examples from tests:**
```java
ExternalItem item = new ExternalItem();
item.setMeta(null);

PlaceholderContext context = PlaceholderContext.create()
    .setPlaceholders(placeholders)
    .with("item", item);

// Null value returns empty Optional
Optional<ExternalMeta> result = context.getPlaceholderValue("item.meta", ExternalMeta.class);
// result.isPresent() = false
```

---

## Custom Placeholder Registration

### Manual Registration

**Examples from tests:**
```java
Placeholders placeholders = Placeholders.create()
    .registerPlaceholder(ExternalItem.class, "type", 
        (item, field, context) -> item.getType())
    .registerPlaceholder(ExternalItem.class, "amount",
        (item, field, context) -> item.getAmount())
    .registerPlaceholder(ExternalItem.class, "meta",
        (item, field, context) -> item.getMeta())
    .registerPlaceholder(ExternalMeta.class, "name",
        (meta, field, context) -> meta.getName());

// Use with context
PlaceholderContext context = placeholders.contextOf(message)
    .with("item", item)
    .apply();
```

### Annotation-Based Registration

Automatically resolves properties via `@Placeholder` annotation:

**Examples from tests:**
```java
// Own schema classes (using @Placeholder annotation)
class Item {
    @Placeholder
    private String type;
    
    @Placeholder
    private int amount;
    
    @Placeholder
    private Meta meta;
    
    // Getters/setters...
}

class Meta {
    @Placeholder
    private String name;
    
    @Placeholder  
    private String lore;
    
    // Getters/setters...
}

// Use directly without manual registration
Item item = new Item();
item.setType("Stone");
item.setAmount(123);

CompiledMessage message = CompiledMessage.of("{item.type} x {item.amount}");
String result = PlaceholderContext.of(message).with("item", item).apply();
// Result: "Stone x 123"
```

---

## Inheritance Support

Placeholder resolvers support class inheritance.

**Examples from tests:**
```java
class X {
    public String getName() { return "John"; }
}

class Y extends X {
    public String getSurname() { return "Paul"; }
}

class Z extends Y {
}

Placeholders placeholders = Placeholders.create()
    .registerPlaceholder(X.class, "name", (e, a, o) -> e.getName())
    .registerPlaceholder(Y.class, "surname", (e, a, o) -> e.getSurname());

// X placeholders work with X
CompiledMessage msg = CompiledMessage.of("{var.name}");
placeholders.contextOf(msg).with("var", new X()).apply();
// Result: "John"

// X placeholders work with Y (which extends X)
CompiledMessage msg = CompiledMessage.of("{var.name} {var.surname}");
placeholders.contextOf(msg).with("var", new Y()).apply();
// Result: "John Paul"

// X placeholders work with Z (which extends Y extends X)
CompiledMessage msg = CompiledMessage.of("{var.name}");
placeholders.contextOf(msg).with("var", new Z()).apply();
// Result: "John"
```

---

## Reflection-Based Placeholders

Dynamic method invocation via reflection.

### Basic Reflection

**Examples from tests:**
```java
Placeholders reflect = ReflectPlaceholders.create();

// Single no-arg method
"{name.toUpperCase()}"
// With name="John": "JOHN"

// Chained methods
"{name.getClass().getSimpleName()}"
// With name="John": "String"
```

### Method with Arguments

**Examples from tests:**
```java
// Two-argument method with literal values
"{name.getClass().getName().replace('a','e')}"
// Result: "jeve.leng.String" (java.lang.String with a->e)

// Method with context values
"{name.getClass().getName().replace(from,to)}"
// With from="a", to="e": "jeve.leng.String"

// Method with integer arguments
"{name.getClass().getName().substring(5,9)}"
// Result: "lang"
```

### Static Members

**Examples from tests:**
```java
class TestType {
    static final String STATIC_STRING = "static string!";
    static String staticMethod() {
        return "static method!";
    }
}

// Static field access
"{test.STATIC_STRING}"
// With test=TestType.class: "static string!"

// Static method access
"{test.staticMethod()}"
// With test=TestType.class: "static method!"
```

### Combined with Built-in Transformations

**Examples from tests:**
```java
Placeholders reflectWithDefaults = ReflectPlaceholders.create(true);

// Reflection + built-in method
"{name.getClass().getName().capitalize}"
// With name="John": "Java.lang.String"
```

---

## Built-in String Transformations

When `Placeholders.create(true)` is used, includes default transformations.

### String Methods

**Examples from tests:**
```java
// toLowerCase
"{item.type.toLowerCase()}"
// "DIAMOND_PICKAXE" -> "diamond_pickaxe"

// replace (with chaining)
"{item.type.replace(_, )}"
// "DIAMOND_PICKAXE" -> "DIAMOND PICKAXE"

// capitalize
"{item.type.replace(_, ).toLowerCase().capitalize()}"
// "DIAMOND_PICKAXE" -> "Diamond pickaxe"
```

### Enum Pretty-Print

**Examples from tests:**
```java
enum Type {
    DIAMOND_PICKAXE
}

// Built-in enum formatter
"{item.typeEnum.pretty}"
// Type.DIAMOND_PICKAXE -> "Diamond Pickaxe"
```

### Number Operations

**Examples from tests:**
```java
// Arithmetic operations
"{item.amount.multiply(0).add(1)}"
// amount=123: "1"
```

---

## Platform-Specific Integrations

### Bukkit Integration

Provides placeholders for Bukkit/Spigot server objects:

**Supported Types:**
- `ChatColor` - Color codes
- `CommandSender` - Command sender info
- `HumanEntity` / `Player` - Player data (health, location, inventory, etc.)
- `Entity` - Entity properties
- `OfflinePlayer` - Offline player data
- `Inventory` / `PlayerInventory` - Inventory data
- `Location` - World coordinates
- And many more (see bukkit/README.md)

**Example from tests:**
```java
// Health bar utility
BukkitPlaceholders.renderHealthBarWith(10, 20, "❤", "c", "7");
// Result: "§c❤❤❤❤❤❤❤❤❤❤§7❤❤❤❤❤❤❤❤❤❤"
```

**Usage:**
```java
// Access player properties
"{player.name}"
"{player.level}"
"{player.health}"
"{player.location.world}"
"{player.inventory.size}"
"{player.bedSpawnLocation|No spawn set}"
```

### Bungee Integration

Provides placeholders for BungeeCord proxy objects:

**Supported Types:**
- `ChatColor` - Color codes
- `CommandSender` - Sender info
- `ProxyServer` - Proxy server data
- `ProxiedPlayer` - Proxied player data
- `Server` - Backend server info

**Usage:**
```java
"{player.displayName}"
"{player.ping}"
"{player.server.name}"
"{server.playersCount}"
```

### PlaceholderAPI Bridge

Integration with Bukkit's PlaceholderAPI:

```java
// Access external placeholders via PlaceholderAPI
"{player.papi(vault_eco_balance)}"
```

---

## Performance Characteristics

### Design Philosophy

1. **Compile Once, Use Many Times**: Messages are compiled to internal representation once
2. **Minimal String Operations**: Avoids string concatenation and chaining during rendering
3. **Lazy Evaluation**: Only processes placeholders that exist in the message
4. **Type Safety**: No string-based type conversions during rendering

### Benchmarks

Based on benchmark module comparisons:

**vs JDK8 String#replace:**
- Faster or comparable in simple cases
- Significantly faster in complex/repeated scenarios
- Safer due to no chaining issues

**vs Apache CommonsLang3 StringUtils:**
- Consistently faster across all scenarios
- Better performance with repeated placeholders
- No performance penalty for static strings

**Benchmark Categories:**
1. **Simple**: Average messages with fields
2. **Repeated**: Multi-line strings with repeated fields
3. **Long**: Few fields in long strings (e.g., email templates)
4. **Static**: No placeholders (tests overhead)

### Performance Notes from Tests

```java
// Recommended: Compile once, reuse
CompiledMessage message = CompiledMessage.of("Hello {who}!");
// ... cache this message ...

// Fast: One-time context when using partial placeholders
PlaceholderContext.of(message).with("who", "World").apply();

// Reusable: When applying same placeholders to multiple messages
PlaceholderContext context = PlaceholderContext.create().with("who", "World");
context.apply(message1);
context.apply(message2);
```

---

## Additional Features

### Fail Modes

Control behavior when placeholders are missing or fail:

```java
// Set fail mode on context
context.setFailMode(FailMode.SKIP);     // Skip missing placeholders
context.setFailMode(FailMode.REPLACE);  // Replace with error text
context.setFailMode(FailMode.THROW);    // Throw exception
```

### Locale Support

Messages can be compiled with specific locales for proper pluralization:

```java
CompiledMessage message = CompiledMessage.of(Locale.ENGLISH, "...");
CompiledMessage message = CompiledMessage.of(Locale.forLanguageTag("pl"), "...");
```

### Unicode Support

Full Unicode support in placeholder names and values:

**Examples from tests:**
```java
MessageField.of("ĆŻĘŚĆ");    // Polish characters
MessageField.of("sęnder");   // Works correctly
```

### Escape Sequences

Support for escaping special characters:

```java
// Escape commas in patterns
"{p,yyyy/MM/dd\\, HH:mm G,Europe/Paris#time}"
```

---

## Summary of Key Features

### Core Strengths

1. **Performance**: Compiled messages, minimal string operations
2. **Type Safety**: Strong typing throughout API
3. **Flexibility**: Supports simple to complex placeholder scenarios
4. **Localization**: Built-in locale support for pluralization and date/time
5. **Extensibility**: Custom placeholder registration, inheritance support
6. **Reflection**: Dynamic method invocation when needed
7. **Platform Integration**: Ready-made integrations for Bukkit and Bungee
8. **Value Extraction**: Programmatic access to placeholder values
9. **Rich Formatting**: Duration, instant, number, boolean formatting
10. **Nested Access**: Deep property access with subfields
11. **Method Parameters**: Call methods with arguments from placeholders
12. **Fallback Values**: Graceful handling of null/missing values

### Use Cases

- **Configuration Messages**: Cache compiled messages, apply with different values
- **Chat/Command Systems**: Player-specific message rendering
- **Notifications**: Templated messages with placeholders
- **Reports/Statistics**: Formatted numbers, durations, timestamps
- **Multilingual Applications**: Locale-aware pluralization and formatting
- **API Integration**: Extract values programmatically without string rendering
- **Performance-Critical**: High-throughput message processing

---

## Testing Coverage

All features documented here are covered by tests in:
- `core/src/test/java/eu/okaeri/placeholderstest/`
  - TestPlaceholderUsage.java - Basic usage
  - TestCompiledMessage.java - Message compilation
  - TestMetadataUsage.java - Pluralization, boolean, number formatting
  - TestDurationPlaceholders.java - Duration formatting
  - TestInstantPlaceholders.java - Date/time formatting
  - TestPlaceholderField.java - Field parsing and nesting
  - TestParamsUsage.java - Method parameters
  - TestPlaceholderInheritance.java - Class inheritance
  - TestPlaceholderValueExtraction.java - Value extraction API
  - TestSchema.java - Schema resolvers and custom placeholders
- `reflect/src/test/java/eu/okaeri/placeholderstest/reflect/`
  - TestReflectPlaceholders.java - Reflection-based placeholders
- `bukkit/src/test/java/eu/okaeri/placeholdersbukkittest/`
  - TestPlaceholderUtils.java - Bukkit utilities

---

## Version Information

This document is based on okaeri-placeholders version 5.1.2 and reflects the features present in the test suite as of the documentation date.
