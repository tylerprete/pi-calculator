package pi

import utest.*

object PixelCoordinateTests extends TestSuite {
  val tests = Tests {
    test("maps square edges to valid pixels") {
      assert(PiCalculator.pixelCoordinate(0.0, 300) == 0)
      assert(PiCalculator.pixelCoordinate(1.0, 300) == 150)
      assert(PiCalculator.pixelCoordinate(1.999, 300) == 299)
      assert(PiCalculator.pixelCoordinate(Math.nextDown(2.0), 300) == 299)
    }
    test("uses the supplied axis size") {
      assert(PiCalculator.pixelCoordinate(1.999, 200) == 199)
      assert(PiCalculator.pixelCoordinate(1.999, 100) == 99)
      assert(PiCalculator.pixelCoordinate(1.999, 1) == 0)
    }
    test("keeps points within their pixel cell") {
      assert(PiCalculator.pixelCoordinate(0.005, 300) == 0)
      assert(PiCalculator.pixelCoordinate(0.01, 300) == 1)
    }
  }
}
