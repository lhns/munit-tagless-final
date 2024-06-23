/*
 * Copyright 2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package munit

import cats.effect.{IO, Resource}
import munit.ResourceFixture.FixtureNotInstantiatedException

import scala.concurrent.duration._

class TaglessFinalFixturesSpec extends CatsEffectSuite with TaglessFinalFixtures[IO] {

  @volatile var acquired: Int = 0
  @volatile var released: Int = 0

  val fixture = ResourceSuiteLocalFixture(
    "fixture",
    Resource.make(
      IO.sleep(1.millis) *> IO {
        acquired += 1
        ()
      }
    )(_ =>
      IO.sleep(1.millis) *> IO {
        released += 1
        ()
      }
    )
  )

  val uninitializedFixture = ResourceSuiteLocalFixture(
    "uninitialized-fixture",
    Resource.make(IO.unit)(_ => IO.unit)
  )

  object AssertBeforeFixture extends Fixture[Unit]("assertBefore") {
    def apply() = ()

    override def beforeAll(): Unit = {
      assertEquals(acquired, 0)
      assertEquals(released, 0)
    }

    override def afterAll(): Unit = {
      assertEquals(acquired, 1)
      assertEquals(released, 0)
    }
  }

  object AssertAfterFixture extends Fixture[Unit]("assertAfter") {
    def apply() = ()

    override def beforeAll(): Unit = {
      assertEquals(acquired, 1)
      assertEquals(released, 0)
    }

    override def afterAll(): Unit = {
      assertEquals(acquired, 1)
      assertEquals(released, 1)
    }
  }

  override def munitFixtures = List[AnyFixture[_]](AssertBeforeFixture, fixture, AssertAfterFixture)

  test("first test") {
    IO(fixture()).assertEquals(())
  }

  test("second test") {
    IO(fixture()).assertEquals(())
  }

  test("throws exception") {
    IO(uninitializedFixture())
      .interceptMessage[FixtureNotInstantiatedException](
        "The fixture `uninitialized-fixture` was not instantiated. Override `munitFixtures` and include a reference to this fixture."
      )
  }

}
