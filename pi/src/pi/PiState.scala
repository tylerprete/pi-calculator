package pi

case class PiState(pointsInCircle: Int, totalPoints: Int) {

  /**
   * If we have a circle with radius of 1 inscribed in a square
   * of sides with length 2, we can calculate pi by taking the ratio formula
   * for area in the circle vs square: pi / 4, and setting that equal to some x.
   *
   * We can come up with a value for x by choosing random points in the square,
   * and figuring out how often they are in the circle.
   *
   * Then we simply solve for pi.
   */
  lazy val pi: Double = {
    4 * pointsInCircle / totalPoints.toDouble
  }

  def next(): (PiState, Point, Boolean) = {
    val randPoint = Point.randomPoint()
    val isInCircle = randPoint.distanceFrom(Point.origin) <= 1.0
    (PiState(if (isInCircle) pointsInCircle + 1 else pointsInCircle, totalPoints + 1), randPoint, isInCircle)
  }
}

object PiState {
  val initialPiState: PiState = PiState(0, 0)
}

