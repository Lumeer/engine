[![Build Status][Travis badge]][Travis build]

[Travis badge]: https://travis-ci.org/Lumeer/engine.svg?branch=devel
[Travis build]: https://travis-ci.org/Lumeer/engine

# engine
Lumeer Backend Engine

## Running tests

### War

Web application tests are switched off by default because they take a lot of time.
To explicitly run them, first make sure you have the most recent build in your local
Maven repository:

```
$ mvn clean install
```

Next switch to `war` directory and run tests disabling the `default` profile:

```
$ mvn verify -P-default
```

Use this to run a specified test method in your test class:

```
$ mvn -Dit.test=YourTestClass#yourTestMethod verify -P-default
```