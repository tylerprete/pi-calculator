// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "2.13.4"
  def scalaJSVersion = "1.3.0"

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::1.1.0"
  )
}
