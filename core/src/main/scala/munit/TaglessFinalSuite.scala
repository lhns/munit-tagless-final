package munit

import cats.effect.kernel.{Async, Deferred, Resource}
import cats.syntax.flatMap._

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class TaglessFinalSuite[F[_]](implicit
                                       private val AsyncF: Async[F],
                                       private val tag: ClassTag[F[_]])
  extends FunSuite
    with TaglessFinalAssertions[F]
    with TaglessFinalFunFixtures[F] {

  protected def toFuture[A](f: F[A]): Future[A]

  private val fTransform: ValueTransform =
    new ValueTransform(
      tag.runtimeClass.getSimpleName,
      { case tag(f: F[_]) => toFuture(f) }
    )

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ fTransform

  private val afterAllF: Deferred[F, F[Unit]] = Deferred.unsafe
  private val beforeAllTest: Test = new Test("before all", () =>
    toFuture(aroundAll().allocated.flatMap(e => afterAllF.complete(e._2))))
  private val afterAllTest: Test = new Test("after all", () => toFuture(afterAllF.get.flatten))

  override def munitTests(): Seq[Test] = beforeAllTest +: super.munitTests() :+ afterAllTest

  /**
    * Resource is allocated once before all test cases and before all suite-local fixtures are setup.
    * An error in the allocation method aborts the test suite.
    * The resource is closed after all test cases and after all suite-local fixtures have been tear down.
    */
  def aroundAll(): Resource[F, Unit] = Resource.unit
}
