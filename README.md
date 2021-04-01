# munit-tagless-final
[![Test Workflow](https://github.com/LolHens/munit-tagless-final/workflows/test/badge.svg)](https://github.com/LolHens/munit-tagless-final/actions?query=workflow%3Atest)
[![Release Notes](https://img.shields.io/github/release/LolHens/munit-tagless-final.svg?maxAge=3600)](https://github.com/LolHens/munit-tagless-final/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/de.lolhens/munit-tagless-final_2.13)](https://search.maven.org/artifact/de.lolhens/munit-tagless-final_2.13)
[![Apache License 2.0](https://img.shields.io/github/license/LolHens/munit-tagless-final.svg?maxAge=3600)](https://www.apache.org/licenses/LICENSE-2.0)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

Integration library for [MUnit](https://scalameta.org/munit/) and any effect type via [cats-effect](https://github.com/typelevel/cats-effect/).

This project is based on [typelevel/munit-cats-effect](https://github.com/typelevel/munit-cats-effect).

### build.sbt
```sbt
// use this snippet for cats-effect 2 and the JVM
libraryDependencies += "de.lolhens" %% "munit-tagless-final" % "0.0.1" % Test

// use this snippet for cats-effect 2 and JS, or cross-building
libraryDependencies += "de.lolhens" %%% "munit-tagless-final" % "0.0.1" % Test

// use this snippet for cats-effect 3 and the JVM
libraryDependencies += "de.lolhens" %% "munit-tagless-final" % "0.1.1" % Test

// use this snippet for cats-effect 3 and JS, or cross-building
libraryDependencies += "de.lolhens" %%% "munit-tagless-final" % "0.1.1" % Test
```

## Example
### Cats Effect 2
```scala
import cats.effect.IO

import scala.concurrent.Future

abstract class CatsEffectSuite extends TaglessFinalSuite[IO] {
  override protected def toFuture[A](f: IO[A]): Future[A] = f.unsafeToFuture()
}
```

### Cats Effect 3
```scala
import cats.effect.{IO, unsafe}

import scala.concurrent.Future

abstract class CatsEffectSuite extends TaglessFinalSuite[IO] {
  override protected def toFuture[A](f: IO[A]): Future[A] = f.unsafeToFuture()(unsafe.IORuntime.global)
}
```

### Monix
```scala
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Future

abstract class TaskSuite extends munit.TaglessFinalSuite[Task] {
  override protected def toFuture[A](f: Task[A]): Future[A] = f.runToFuture(Scheduler.global)
}
```

## License
This project uses the Apache 2.0 License. See the file called LICENSE.
