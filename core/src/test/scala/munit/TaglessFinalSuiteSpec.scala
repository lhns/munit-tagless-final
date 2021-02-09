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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package munit

import cats.effect.{ContextShift, IO, Timer}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class TaglessFinalSuiteSpec extends CatsEffectSuite {

  implicit def munitTimer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  test("nested IO fail".fail) {
    IO {
      IO.sleep(2.millis)
        .flatMap { _ =>
          IO {
            IO.sleep(2.millis)
              .flatMap { _ =>
                IO {
                  IO.sleep(2.millis)
                    .map(_ => assertEquals(false, true))
                }
              }
          }
        }
    }
  }
  test("nested IO success") {
    IO {
      IO.sleep(2.millis)
        .flatMap { _ =>
          IO {
            IO.sleep(2.millis)
              .flatMap { _ =>
                IO {
                  IO.sleep(2.millis)
                    .map(_ => assertEquals(true, true))
                }
              }
          }
        }
    }
  }
}
