package pi

import scala.util.Random

case class Point(x: Double, y: Double) {
  def distanceFrom(other: Point): Double = {
    val xdist = x - other.x
    val ydist = y - other.y
    math.sqrt(xdist * xdist + ydist * ydist)
  }
}

object Point {
  def randomPoint(): Point = {
    val x = Random.nextDouble() * 2D
    val y = Random.nextDouble() * 2D
    Point(x, y)
  }

  val origin: Point = Point(1D, 1D)
}

