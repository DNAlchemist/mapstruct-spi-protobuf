# Mapstruct SPI implementation for protocol buffers [![CircleCI](https://circleci.com/gh/entur/mapstruct-spi-protobuf.svg?style=svg)](https://circleci.com/gh/entur/mapstruct-spi-protobuf)


---

## This is a fork of the original project
### Fork notes

The MapStruct SPI library has a problem with Protobuf enums
It uses the enums from the Protobuf contract for mapping.

But if the consumer's contract is outdated and doesn’t include a new enum value from the producer, the mapping will set the value to `UNRECOGNIZED`.
This causes data loss and breaks Protobuf’s backward compatibility.

To fix this, we need to allow the library to handle enums as raw integer values so that even unknown values are not lost
Example code generation of maptruct:

Producer contract
```protobuf
enum UserEnum {
    USER_ENUM_FIRST = 1;
    USER_ENUM_SECOND = 2;
};
```

Consumer contract
```protobuf
enum UserEnum {
    USER_ENUM_FIRST = 1;
};
```

When producer sent `USER_ENUM_SECOND` value

Expected Consumer MapStruct Generated Result
```java
proto.getUserEnumValue() // 2 (Method get enum value operated with raw int value without any wrappers, so we didn't lost any new values that not presented in contract)
```
Actual Consumer MapStruct Generated Result
```java
proto.getUserEnum().getNumber() // -1 (Not presented in contract, UNRECOGNIZED enum value)
```

To fix this, we need to allow the library to handle enums as raw integer values so that even unknown values are not lost
With this fork, we can use the raw integer value of the enum in the mapping, by enabling the flag `mapstructSpi.useEnumRawValue` as a compilerArg in the format of:

`-AmapstructSpi.useEnumRawValue=true`

### ! Important !
This option changes mapping way of enums
Enum will be mapped as raw integer value, not by enum name
In this way will be used ordingal value of enums
So if you using enum to enum mapping, these enums should have the same order of values

### Maven example
```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>io.github.dnalchemist.mapstruct.spi</groupId>
                <artifactId>protobuf-spi-impl</artifactId>
                <version>LATEST.VERSION</version>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-AmapstructSpi.useEnumRawValue=true</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### Gradle example
```groovy
dependencies {
    annotationProcessor "io.github.dnalchemist.mapstruct.spi:protobuf-spi-impl:LATEST.VERSION"
}

compileJava {
    options.compilerArgs += [
            "-AmapstructSpi.useEnumRawValue=true"
    ]
}
```

### Release cheetsheet
```shell
# install gpg
brew install gpg
# generate gpg key
gpg --gen-key
# list key-ids
gpg --list-keys
# export key
gpg --keyserver keys.openpgp.org --send-keys <key-id>
# sign the release and deploy
mvn clean deploy
```

---

This naming strategy helps [mapstruct](http://mapstruct.org/) generate working mapping code between your domain classes
and protobuf classes. Both [fullblown Java protobuf](https://github.com/protocolbuffers/protobuf/tree/master/java)
and [protolite](https://github.com/protocolbuffers/protobuf/blob/master/java/lite.md) classes suported.

Requires on mapstruct 1.4.0+.

## ProtobufAccessorNamingStrategy

Extends ```DefaultProtobufAccessorNamingStrategy``` and provides necessary information to map all fields automatically *
except*

* oneof

which require manual mapping.

## ProtobufEnumMappingStrategy

Implements ```EnumMappingStrategy``` and provides complete enum constant mappings if you follow Googles style guide for
enums https://developers.google.com/protocol-buffers/docs/style#enums

If needed you can specify a different postfix for the 0 value enum by passing in `mapstructSpi.enumPostfixOverrides` as
a compilerArg in the format of:

`-AmapstructSpi.enumPostfixOverrides=com.package.root.a=POSTFIX_1,com.package.root.b=POSTFIX_2`

Otherwise, this will default to `UNSPECIFIED` as per the Google style guide.

```xml

<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>no.entur.mapstruct.spi</groupId>
                <artifactId>protobuf-spi-impl</artifactId>
                <version>LATEST.VERSION</version>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-AmapstructSpi.enumPostfixOverrides=com.company.name=INVALID</arg>
        </compilerArgs>
    </configuration>
</plugin>

```

## Support - Mapping functions:

Standard mapping functions between often used proto types and java types:

* Timestamp <-> Instant
* Duration <-> Duration
* Date <-> LocalDate
* TimeOfDay <-> LocalTime
* byte[] <-> ByteString

See [protobuf-support-standard](support-standard) and/or [protobuf-support-lite](support-lite) folders for a
ready-to-use mapstruct mapper.

# Usage

[See example project](usage/)

NB: Make sure you add `collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED` to your mapping interfaces
as protobuf stubs use the builder pattern.

```
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface ... {
```

## Maven

NB: Make sure you use the *same version of mapstruct* both in the annotation process and the general dependency.
`mapstruct-spi-protobuf` generally depends on the latest released version of mapstruct.

Add the following section to you maven-compiler-plugin plugin configuration:

```xml

<annotationProcessorPaths>
    <path>
        <groupId>no.entur.mapstruct.spi</groupId>
        <artifactId>protobuf-spi-impl</artifactId> <!-- Make sure mapstruct version here is the same as below -->
        <version>LATEST.VERSION</version>
    </path>
</annotationProcessorPaths>
<dependencies>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${org.mapstruct.version}</version>
</dependency>
</dependencies>

```

Complete example:

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>

    <configuration>
        <source>1.8</source>
        <target>1.8</target>
        <annotationProcessorPaths>
            <path>
                <groupId>no.entur.mapstruct.spi</groupId>
                <artifactId>protobuf-spi-impl</artifactId>
                <version>LATEST.VERSION</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${org.mapstruct.version}</version>
        </dependency>
    </dependencies>

</plugin>
```

## Gradle

Note: See Maven setup regarding using the same version of mapstruct both in the annotation processor and the general
dependency.

```java
implementation"org.mapstruct:mapstruct:${mapstructVersion}"
        annotationProcessor"org.mapstruct:mapstruct-processor:${mapstructVersion}"
        annotationProcessor"no.entur.mapstruct.spi:protobuf-spi-impl:LATEST.VERSION"
```

# More information:

http://mapstruct.org/documentation/stable/reference/html/index.html#using-spi

