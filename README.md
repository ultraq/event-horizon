
Event Horizon
=============

[![build](https://github.com/ultraq/event-horizon/actions/workflows/build.yml/badge.svg)](https://github.com/ultraq/event-horizon/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/ultraq/event-horizon/graph/badge.svg?token=3A4WLOZ30M)](https://codecov.io/gh/ultraq/event-horizon)
[![Maven Central Version](https://img.shields.io/maven-central/v/nz.net.ultraq/event-horizon)](https://central.sonatype.com/artifact/nz.net.ultraq/event-horizon)

An async, in-process, event system.  Originally, the event system for the
[Red Horizon](https://github.com/ultraq/redhorizon) game engine, hence the name.


Installation
------------

Minimum of Java 17 required.

Add a dependency to your project with the following co-ordinates:

- GroupId: `nz.net.ultraq`
- ArtifactId: `event-horizon`
- Version: `0.1.0`

Check the [project tags](https://github.com/ultraq/event-horizon/tags) for a
list of available versions.


Usage
-----

Firstly, the class that will want to emit events should implement the
`EventTarget` interface.  Then, anywhere within an instance of that class, it
can call the `trigger` method to emit an event:


```java
import nz.net.ultraq.eventhorizon.EventTarget;

public class MyClass implements EventTarget {

  public void myMethod() {
    trigger(new GreetingEvent("Hello!"));
  }
}
```

Event objects are any class that implements the `Event` interface, and can
contain any data you wish to pass along with an event.  Java `record`s make for
really succint event types:

```java
import nz.net.ultraq.eventhorizon.Event;

public record GreetingEvent(String greeting) implements Event {}
```

Finally, any code in your codebase can listen to and react to events by using
the `on` method on that `EventTarget` instance:

```java
public void somewhereElse(MyClass myClass) {
  myClass.on(GreetingEvent.class, event -> {
    System.out.println("MyClass says " + event.greeting());
  });
}
```

There are a few more methods for different use cases, which you can find in the
groovydocs here: https://javadoc.io/doc/nz.net.ultraq/event-horizon
