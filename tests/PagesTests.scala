//| extends: mill.script.ScalaModule.Utest
//| scalaVersion: 3.10.0
//| jvmVersion: 21
//| moduleDeps: [//.github/scripts/Pages.scala]
//| mvnDeps: [com.lihaoyi::utest:0.9.5]
package pages

import java.nio.file.{Files, Path}
import utest.*

object PagesTests extends TestSuite {
  class Fixture {
    val root: Path = Files.createTempDirectory("pages-test-")
    val existing: Path = root.resolve("live")
    val built: Path = root.resolve("build")
    val output: Path = root.resolve("output")

    def write(path: Path, content: String): Unit = {
      Files.createDirectories(path.getParent)
      Files.writeString(path, content)
    }
    def read(relative: String): String = Files.readString(output.resolve(relative))
    def exists(relative: String): Boolean = Files.exists(output.resolve(relative))
    def prepare(mode: String, number: String = "42"): Unit = {
      Pages.prepare(mode, number, existing, built, output)
    }

    write(existing.resolve("index.html"), "production")
    write(existing.resolve("obsolete.js"), "old asset")
    write(existing.resolve("previews/pr-42/index.html"), "old preview")
    write(existing.resolve("previews/pr-43/index.html"), "other preview")
    write(existing.resolve(".git/config"), "must not be published")
    write(built.resolve("index.html"), "new build")
    write(built.resolve("js/app.js"), "new JS")
  }

  def withFixture(check: Fixture => Unit): Unit = {
    val fixture = new Fixture
    try check(fixture) finally Pages.remove(fixture.root)
  }

  def run(event: String = "push"): ujson.Value = ujson.Obj(
    "conclusion" -> "success", "path" -> ".github/workflows/scala.yml",
    "event" -> event, "head_branch" -> "master", "head_sha" -> "current"
  )

  val tests = Tests {
    test("preview preserves production and other previews") {
      withFixture { f =>
        f.write(f.existing.resolve("previews/pr-42/stale.js"), "old")
        f.prepare("preview")
        assert(f.read("index.html") == "production")
        assert(f.read("previews/pr-42/index.html") == "new build")
        assert(f.read("previews/pr-42/js/app.js") == "new JS")
        assert(f.read("previews/pr-43/index.html") == "other preview")
        assert(!f.exists("previews/pr-42/stale.js"), !f.exists(".git"))
        assert(Files.isRegularFile(f.output.resolve(".nojekyll")))
      }
    }
    test("production preserves previews and removes stale assets") {
      withFixture { f =>
        f.prepare("production", "")
        assert(f.read("index.html") == "new build", f.read("js/app.js") == "new JS")
        assert(f.read("previews/pr-42/index.html") == "old preview")
        assert(!f.exists("obsolete.js"))
      }
    }
    test("cleanup only removes selected preview") {
      withFixture { f =>
        Pages.remove(f.built)
        f.prepare("cleanup")
        assert(!f.exists("previews/pr-42"), f.exists("previews/pr-43/index.html"))
        assert(f.read("index.html") == "production")
      }
    }
    test("cleanup missing preview is safe") {
      withFixture { f =>
        f.prepare("cleanup", "99")
        assert(f.exists("previews/pr-42/index.html"))
      }
    }
    test("rejects invalid modes and PR destinations") {
      withFixture { f =>
        assertThrows[IllegalArgumentException] { f.prepare("invalid") }
        for (number <- Seq("../../production", "", "0", "-1", "01", "42\n")) {
          assertThrows[IllegalArgumentException] { f.prepare("preview", number) }
          assertThrows[IllegalArgumentException] { f.prepare("cleanup", number) }
        }
        assert(!Files.exists(f.output))
      }
    }
    test("rejects symlinks in artifact and live tree") {
      withFixture { f =>
        for (tree <- Seq(f.built, f.existing)) {
          val link = tree.resolve("linked")
          Files.createSymbolicLink(link, f.existing.resolve(".git/config"))
          assertThrows[IllegalArgumentException] { f.prepare("preview") }
          Files.delete(link)
        }
        assert(!Files.exists(f.output))
      }
    }
    test("rejects root and dangling symlinks") {
      withFixture { f =>
        val link = f.root.resolve("linked-build")
        Files.createSymbolicLink(link, f.built)
        assertThrows[IllegalArgumentException] { Pages.validateTree(link) }
        Files.createSymbolicLink(f.built.resolve("dangling"), f.root.resolve("missing"))
        assertThrows[IllegalArgumentException] { f.prepare("preview") }
      }
    }
    test("rejects hidden control files and reserved directory") {
      withFixture { f =>
        for (name <- Seq(".git/config", ".gitattributes", "js/.git/config", "previews/index.html")) {
          val path = f.built.resolve(name)
          f.write(path, "untrusted")
          assertThrows[IllegalArgumentException] { f.prepare("preview") }
          Pages.remove(f.built.resolve(name.split("/").head))
        }
      }
    }
    test("rejects missing index") {
      withFixture { f =>
        Files.delete(f.built.resolve("index.html"))
        assertThrows[IllegalArgumentException] { f.prepare("preview") }
      }
    }
    test("accepts nojekyll marker") {
      withFixture { f =>
        f.write(f.built.resolve(".nojekyll"), "")
        f.prepare("production", "")
        assert(f.exists(".nojekyll"))
      }
    }
    test("selection skips outdated production") {
      val workflow = run()
      val get: Pages.Api = (repo, endpoint, paginate) => {
        assert(repo == "owner/repo", endpoint == "commits/master", !paginate)
        ujson.Obj("sha" -> "current")
      }
      val event = ujson.Obj("workflow_run" -> workflow)
      assert(Pages.select(event, "owner/repo", get) == Pages.Target("production"))
      workflow("head_sha") = "outdated"
      assert(Pages.select(event, "owner/repo", get) == Pages.Target("skip"))
    }
    test("fork selection without embedded PR metadata") {
      val workflow = run("pull_request")
      workflow("head_branch") = "feature"
      workflow("pull_requests") = ujson.Arr()
      val pr = ujson.Obj("number" -> 42, "head" ->
        ujson.Obj("sha" -> "current", "ref" -> "feature"))
      val get: Pages.Api = (repo, endpoint, paginate) => {
        assert(repo == "owner/repo", endpoint == "pulls?state=open&base=master&per_page=100", paginate)
        ujson.Arr(pr)
      }
      val event = ujson.Obj("workflow_run" -> workflow)
      assert(Pages.select(event, "owner/repo", get) == Pages.Target("preview", "42"))
      pr("head")("sha") = "newer"
      assert(Pages.select(event, "owner/repo", get) == Pages.Target("skip"))
      val empty: Pages.Api = (_, _, _) => ujson.Arr()
      assert(Pages.select(event, "owner/repo", empty) == Pages.Target("skip"))
    }
    test("ambiguous PR heads are skipped") {
      val event = ujson.Obj("workflow_run" -> run("pull_request"))
      val get: Pages.Api = (_, _, _) => ujson.Arr.from(Seq(42, 43).map { number =>
        ujson.Obj("number" -> number, "head" -> ujson.Obj("sha" -> "current", "ref" -> "master"))
      })
      assert(Pages.select(event, "owner/repo", get) == Pages.Target("skip"))
    }
    test("failed or unrelated runs are not published") {
      val noApi: Pages.Api = (_, _, _) => throw new java.lang.AssertionError("Unexpected API call")
      val failed = ujson.Obj("workflow_run" -> ujson.Obj("conclusion" -> "failure"))
      assert(Pages.select(failed, "owner/repo", noApi) == Pages.Target("skip"))
      val workflow = run()
      workflow("path") = ".github/workflows/other.yml"
      assert(Pages.select(ujson.Obj("workflow_run" -> workflow), "owner/repo", noApi) == Pages.Target("skip"))
    }
    test("reopened PR is not cleaned up") {
      val event = ujson.Obj("action" -> "closed", "pull_request" -> ujson.Obj("number" -> 42))
      val closed: Pages.Api = (_, endpoint, paginate) => {
        assert(endpoint == "pulls/42", !paginate)
        ujson.Obj("state" -> "closed")
      }
      val open: Pages.Api = (_, _, _) => ujson.Obj("state" -> "open")
      assert(Pages.select(event, "owner/repo", closed) == Pages.Target("cleanup", "42"))
      assert(Pages.select(event, "owner/repo", open) == Pages.Target("skip"))
    }
  }
}
