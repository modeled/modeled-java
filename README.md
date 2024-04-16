# MODELED :: Java

> Bringing **M**odel-driven **O**bject-oriented `#coding`
with **D**eclarative, **E**vent-based, **L**ightweight,
& **E**xtensible **D**esign to [Java](https://oracle.com/java) —&
to Java interop in [Python](https://python.org)
via [Jep](https://github.com/ninia/jep)

### For Java it provides:

* a complement to [Lombok](https://projectlombok.org) — though
  based on generated code instead of AST manipulation — fueling
  properties with `Optional`, `Stream` /
  [StreamEx](https://github.com/amaembo/streamex), `Future`,
  & event-based access
* a foundation for connecting Java classes more intuitively
  with data model languages, with data storage, serialization,
  & with user interface technologies, etc.

### For Python, things are yet TODO

## Setup // how-to add it to your project

### Java :: Gradle

* https://docs.gradle.org/current/userguide/declaring_dependencies.html

**Groovy** DSL —`build.gradle`

```groovy
dependencies {
    annotationProcessor group: 'me.modeled', name: 'modeled-java', version: '0.1.0-SNAPSHOT'
    implementation group: 'me.modeled', name: 'modeled-java', version: '0.1.0-SNAPSHOT'
}
```

**Kotlin** DSL —`build.gradle.kts`

```kotlin
dependencies {
    annotationProcessor(group = "me.modeled", name = "modeled-java", version = "0.1.0-SNAPSHOT")
    implementation(group = "me.modeled", name = "modeled-java", version = "0.1.0-SNAPSHOT")
}
```

* **OR** through `gradle/libs.versions.toml`—
  https://docs.gradle.org/current/userguide/platforms.html#sub:conventional-dependencies-toml

```toml
[versions]
modeled-java = '0.1.0-SNAPSHOT'

[libraries]
modeled-java = { group = 'me.modeled', name = 'modeled-java', version.ref = 'modeled-java' }
```

**Groovy** DSL —`build.gradle`

```groovy
dependencies {
    annotationProcessor libs.modeled.java
    implementation libs.modeled.java
}
```

**Kotlin** DSL —`build.gradle.kts`

```kotlin
dependencies {
    annotationProcessor(libs.modeled.java)
    implementation(libs.modeled.java)
}
```

### Java :: Maven —`pom.xml`

* https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html

```xml
<dependency>
    <groupId>me.modeled</groupId>
    <artifactId>modeled-java</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## `#coding` // how-to use it in your project

### Java :: `@Modeled` classes, `@Modeled.Property` fields

For every `@Modeled` class,
a `sealed` & `_Model`-suffixed interface is generated,
which defines a whole variety of `default` access methods — getters,
setters, checkers, & requirers; mappers, streamers, listeners, etc. — for
every `@Modeled.Property` defined within the `@Modeled` class:

```java
import me.modeled.Modeled;
```
