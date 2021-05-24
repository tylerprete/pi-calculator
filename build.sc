// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "3.0.0"
  def scalaJSVersion = "1.5.1"

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::1.1.0".withDottyCompat(scalaVersion())
  )
}
