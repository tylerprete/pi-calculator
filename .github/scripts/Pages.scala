//| extends: mill.script.ScalaModule.Raw
//| scalaVersion: 3.10.0
//| jvmVersion: 25
//| mainClass: pages.Pages
//| mvnDeps:
//| - com.lihaoyi::os-lib:0.11.8
//| - com.lihaoyi::ujson:4.4.3
package pages

import java.nio.file.{Files, LinkOption, Path, StandardOpenOption}
import scala.jdk.CollectionConverters.*

/** Select trusted deployment destinations and assemble the complete Pages tree.
  * PR artifacts are static data: never import or execute anything from them.
  */
object Pages {
  case class Target(mode: String, number: String = "")

  type Api = (String, String, Boolean) => ujson.Value

  def api(repository: String, endpoint: String, paginate: Boolean): ujson.Value = {
    val command = Seq("gh", "api", s"repos/$repository/$endpoint") ++
      (if (paginate) Seq("--paginate", "--slurp") else Seq.empty)
    val result = ujson.read(os.proc(command).call().out.text())
    if (paginate) ujson.Arr.from(result.arr.flatMap(_.arr)) else result
  }

  def select(event: ujson.Value, repository: String, get: Api): Target = {
    if (event.obj.contains("pull_request")) {
      val number = event("pull_request")("number").num.toLong.toString
      val pr = get(repository, s"pulls/$number", false)
      if (event("action").str == "closed" && pr("state").str == "closed") {
        Target("cleanup", number)
      } else Target("skip")
    } else {
      val run = event("workflow_run")
      if (run("conclusion").str != "success" ||
          run("path").str != ".github/workflows/scala.yml") {
        Target("skip")
      } else if (run("event").str == "push") {
        if (run("head_branch").str == "master" &&
            run("head_sha").str == get(repository, "commits/master", false)("sha").str) {
          Target("production")
        } else Target("skip")
      } else if (run("event").str == "pull_request") {
        // Fork runs can omit PR metadata. Match current open PR heads using
        // trusted API data, never a destination from the untrusted artifact.
        val prs = get(repository, "pulls?state=open&base=master&per_page=100", true)
        val matches = prs.arr.filter { pr =>
          pr("head")("sha").str == run("head_sha").str &&
            pr("head")("ref").str == run("head_branch").str
        }
        if (matches.size == 1) {
          Target("preview", matches.head("number").num.toLong.toString)
        } else Target("skip")
      } else Target("skip")
    }
  }

  private def entries(directory: Path): Seq[Path] = {
    val stream = Files.list(directory)
    try stream.iterator().asScala.toVector finally stream.close()
  }

  def validateTree(directory: Path, checkout: Boolean = false): Unit = {
    require(!Files.isSymbolicLink(directory) && Files.isDirectory(directory),
      s"Not a directory: $directory")
    for (entry <- entries(directory)) {
      val name = entry.getFileName.toString
      if (!(checkout && name == ".git")) {
        require(!Files.isSymbolicLink(entry), s"Symlink in site: $entry")
        require(!name.startsWith(".") || name == ".nojekyll",
          s"Hidden control file in site: $entry")
        if (Files.isDirectory(entry)) validateTree(entry)
        else require(Files.isRegularFile(entry), s"Not a regular file: $entry")
      }
    }
  }

  def remove(path: Path): Unit = {
    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      entries(path).foreach(remove)
    }
    Files.deleteIfExists(path)
  }

  private def copyTree(source: Path, destination: Path, checkout: Boolean = false): Unit = {
    Files.createDirectories(destination)
    for (entry <- entries(source)) {
      if (!(checkout && entry.getFileName.toString == ".git")) {
        val target = destination.resolve(entry.getFileName)
        if (Files.isDirectory(entry)) copyTree(entry, target)
        else Files.copy(entry, target)
      }
    }
  }

  def prepare(mode: String, number: String, existing: Path, built: Path, output: Path): Unit = {
    require(Set("production", "preview", "cleanup").contains(mode), s"Invalid mode: $mode")
    require(mode == "production" || number.matches("[1-9][0-9]*"), "Invalid PR number")
    validateTree(existing, checkout = true)
    if (mode != "cleanup") {
      validateTree(built)
      require(Files.isRegularFile(built.resolve("index.html")), "Build is missing index.html")
      require(!Files.exists(built.resolve("previews")), "Build uses reserved previews directory")
    }
    require(!Files.exists(output, LinkOption.NOFOLLOW_LINKS), s"Output already exists: $output")

    // Start with the live tree so previews never replace production files.
    copyTree(existing, output, checkout = true)
    if (mode == "production") {
      entries(output).filter(_.getFileName.toString != "previews").foreach(remove)
      copyTree(built, output)
    } else {
      val destination = output.resolve("previews").resolve(s"pr-$number")
      remove(destination)
      if (mode == "preview") copyTree(built, destination)
    }
    Files.writeString(output.resolve(".nojekyll"), "",
      StandardOpenOption.CREATE, StandardOpenOption.APPEND)
  }

  def main(args: Array[String]): Unit = {
    require(args.length == 1, "Expected select or prepare")
    args(0) match {
      case "select" =>
        val event = ujson.read(Files.readString(Path.of(sys.env("GITHUB_EVENT_PATH"))))
        val target = select(event, sys.env("GITHUB_REPOSITORY"), api)
        Files.writeString(Path.of(sys.env("GITHUB_OUTPUT")),
          s"mode=${target.mode}\nnumber=${target.number}\n",
          StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        val suffix = if (target.number.nonEmpty) s" for PR #${target.number}" else ""
        println(s"Deployment: ${target.mode}$suffix")
      case "prepare" =>
        prepare(sys.env("MODE"), sys.env.getOrElse("PR_NUMBER", ""),
          Path.of("existing-site"), Path.of("built-public"), Path.of("published-site"))
      case _ => throw new IllegalArgumentException("Expected select or prepare")
    }
  }
}
