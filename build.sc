// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "2.13.2"
  def scalaJSVersion = "1.0.1"

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::1.0.0"
  )
}
