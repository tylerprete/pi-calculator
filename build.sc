// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "3.2.1"
  def scalaJSVersion = "1.12.0"

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::2.3.0"
  )
}
