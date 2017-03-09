[![Build Status][Travis badge]][Travis build]

[Travis badge]: https://travis-ci.org/Lumeer/engine.svg?branch=devel
[Travis build]: https://travis-ci.org/Lumeer/engine

# Lumeer engine
Lumeer Backend Engine

## Running tests

### Unit tests

Unit tests are automatically run every time you build the project using the following command:

```
$ mvn clean install
```

You may also want to run just a single test class:

```
$ mvn clean test -Dtest=YourTestClass
```

... or a specific method:

```
$ mvn clean test -Dtest=YourTestClass#yourTestMethod
```

### Integration tests

Integration tests are the ones which needs a container (WildFly) to run.
They are switched off by default because it takes more time to run them.
To explicitly run them, use the following command:

```
$ mvn clean verify -P-default
```

You can also run just a single test class or a specific method:

```
$ mvn clean verify -P-default -Dit.test=YourTestClass#yourTestMethod
```
