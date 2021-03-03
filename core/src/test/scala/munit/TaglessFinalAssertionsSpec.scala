/*
 * Copyright 2020 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITFNS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package munit

import cats.effect.IO
import cats.syntax.all._

import scala.concurrent.duration._

class TaglessFinalAssertionsSpec extends CatsEffectSuite {

  test("assertF works (successful assertion)") {
    val io = IO.sleep(2.millis) *> IO(2)

    assertF(io, returns = 2)
  }
  test("assertF works (failed assertion)".fail) {
    val io = IO.sleep(2.millis) *> IO(2)

    assertF(io, returns = 3)
  }

  test("interceptF works (successful assertion)") {
    val io = (new IllegalArgumentException("BOOM!")).raiseError[IO, Unit]

    interceptF[IllegalArgumentException](io)
  }

  test("interceptF works (failed assertion: different exception)".fail) {
    val io = IO(fail("BOOM!"))

    interceptF[IllegalArgumentException](io)
  }

  test("interceptF works (sucessful assertion on `FailException`)") {
    val io = IO(fail("BOOM!"))

    interceptF[FailException](io)
  }

  test("interceptF works (failed assertion: F does not fail)".fail) {
    val io = IO(42)

    interceptF[IllegalArgumentException](io)
  }

  test("interceptMessageF works (successful assertion)") {
    val io = (new IllegalArgumentException("BOOM!")).raiseError[IO, Unit]

    interceptMessageF[IllegalArgumentException]("BOOM!")(io)
  }

  test("interceptMessageF works (failed assertion: different exception)".fail) {
    val io = IO(fail("BOOM!"))

    interceptMessageF[IllegalArgumentException]("BOOM!")(io)
  }

  test("interceptMessageF works (failed assertion: different message)".fail) {
    val io = IO(fail("BOOM!"))

    interceptMessageF[IllegalArgumentException]("BOOM!")(io)
  }

  test("interceptMessageF works (failed assertion: F does not fail)".fail) {
    val io = IO(42)

    interceptMessageF[IllegalArgumentException]("BOOM!")(io)
  }

}
