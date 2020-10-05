// build.sc
import mill._, scalalib._, scalajslib._

object pi extends ScalaJSModule {
  def scalaVersion = "2.13.3"
  def scalaJSVersion = "1.2.0"

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::1.1.0"
  )
}
