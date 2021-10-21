package pi

/**
 * Encapsulates a Color, allowing you to do useful work with it
 * before serializing it to a String
 */
case class Color(r: Int, g: Int, b: Int) {
  override def toString() = s"rgb($r, $g, $b)"

  def toHex: String = f"#$r%02x$g%02x$b%02x"

  def *(c: Color) = Color(r * c.r, g * c.g, b * c.b)

  def +(c: Color) = Color(r + c.r, g + c.g, b + c.b)
}

object Color {

  val d = "[0-9a-zA-Z]"
  val RGB = "rgb\\((\\d+), (\\d+), (\\d+)\\)".r
  val ShortHex = s"#($d)($d)($d)".r
  val LongHex = s"#($d$d)($d$d)($d$d)".r

  def hex(x: String) = Integer.parseInt(x, 16)

  def apply(s: String): Color = {
    s match {
      case RGB(r, g, b)      => Color(r.toInt, g.toInt, b.toInt)
      case ShortHex(r, g, b) => Color(hex(r) * 16, hex(g) * 16, hex(b) * 16)
      case LongHex(r, g, b)  => Color(hex(r), hex(g), hex(b))
    }
  }

  val White = Color(255, 255, 255)
  val Red = Color(255, 0, 0)
  val Green = Color(0, 255, 0)
  val Blue = Color(0, 0, 255)
  val Cyan = Color(0, 255, 255)
  val Magenta = Color(255, 0, 255)
  val Yellow = Color(255, 255, 0)
  val Black = Color(0, 0, 0)
  val all = Seq(
      White,
      Red,
      Green,
      Blue,
      Cyan,
      Magenta,
      Yellow,
      Black
  )
}
