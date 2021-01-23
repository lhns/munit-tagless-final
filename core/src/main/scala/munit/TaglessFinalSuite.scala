package munit

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class TaglessFinalSuite[F[_]](implicit private val tag: ClassTag[F[_]])
  extends FunSuite
    with TaglessFinalAssertions[F]
    with TaglessFinalFunFixtures[F] {

  protected def toFuture[A](f: F[A]): Future[A]

  private val fTransform: ValueTransform =
    new ValueTransform(
      tag.runtimeClass.getSimpleName,
      { case f: F[_] => toFuture(f) }
    )

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ fTransform
}
