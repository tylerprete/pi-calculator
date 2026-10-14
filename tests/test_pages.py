"""Regression checks for production/preview isolation and deployment selection."""

import importlib.util
from pathlib import Path
import tempfile
import unittest


spec = importlib.util.spec_from_file_location(
    "pages", Path(__file__).resolve().parents[1] / ".github/scripts/pages.py"
)
pages = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pages)


class PagesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.existing, self.built, self.output = [root / name for name in ("live", "build", "output")]
        self.write(self.existing / "index.html", "production")
        self.write(self.existing / "obsolete.js", "old asset")
        self.write(self.existing / "previews/pr-42/index.html", "old preview")
        self.write(self.existing / "previews/pr-43/index.html", "other preview")
        self.write(self.existing / ".git/config", "must not be published")
        self.write(self.built / "index.html", "new build")
        self.write(self.built / "js/app.js", "new JS")

    def write(self, path, content):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)

    def prepare(self, mode, number="42"):
        pages.prepare(mode, number, self.existing, self.built, self.output)

    def test_preview_preserves_production_and_other_previews(self):
        self.write(self.existing / "previews/pr-42/stale.js", "old")
        self.prepare("preview")
        self.assertEqual((self.output / "index.html").read_text(), "production")
        self.assertEqual((self.output / "previews/pr-42/index.html").read_text(), "new build")
        self.assertEqual((self.output / "previews/pr-43/index.html").read_text(), "other preview")
        self.assertFalse((self.output / "previews/pr-42/stale.js").exists())
        self.assertFalse((self.output / ".git").exists())
        self.assertTrue((self.output / ".nojekyll").is_file())

    def test_production_preserves_previews_and_removes_stale_assets(self):
        self.prepare("production", "")
        self.assertEqual((self.output / "index.html").read_text(), "new build")
        self.assertEqual((self.output / "previews/pr-42/index.html").read_text(), "old preview")
        self.assertFalse((self.output / "obsolete.js").exists())

    def test_cleanup_only_removes_selected_preview(self):
        self.prepare("cleanup")
        self.assertFalse((self.output / "previews/pr-42").exists())
        self.assertTrue((self.output / "previews/pr-43/index.html").exists())
        self.assertEqual((self.output / "index.html").read_text(), "production")

    def test_cleanup_missing_preview_is_safe(self):
        self.prepare("cleanup", "99")
        self.assertTrue((self.output / "previews/pr-42/index.html").exists())

    def test_rejects_destination_traversal(self):
        with self.assertRaises(ValueError):
            self.prepare("preview", "../../production")

    def test_rejects_symlinks_in_artifact_and_live_tree(self):
        for tree in (self.built, self.existing):
            with self.subTest(tree=tree):
                link = tree / "linked"
                link.symlink_to(self.existing / ".git/config")
                with self.assertRaises(ValueError):
                    self.prepare("preview")
                link.unlink()

    def test_rejects_git_control_files_and_reserved_directory(self):
        for name in (".git/config", ".gitattributes", "previews/index.html"):
            with self.subTest(name=name):
                path = self.built / name
                self.write(path, "untrusted")
                with self.assertRaises(ValueError):
                    self.prepare("preview")
                pages.remove(self.built / name.split("/")[0])

    def test_rejects_missing_index(self):
        (self.built / "index.html").unlink()
        with self.assertRaises(ValueError):
            self.prepare("preview")

    def test_selection_skips_outdated_production(self):
        run = {"conclusion": "success", "path": ".github/workflows/scala.yml",
               "event": "push", "head_branch": "master", "head_sha": "current"}
        get = lambda repo, endpoint: {"sha": "current"}
        self.assertEqual(pages.select({"workflow_run": run}, "owner/repo", get), ("production", ""))
        run["head_sha"] = "outdated"
        self.assertEqual(pages.select({"workflow_run": run}, "owner/repo", get), ("skip", ""))

    def test_fork_selection_without_embedded_pr_metadata(self):
        run = {"conclusion": "success", "path": ".github/workflows/scala.yml",
               "event": "pull_request", "head_branch": "feature", "head_sha": "current",
               "pull_requests": []}
        pr = {"number": 42, "head": {"sha": "current", "ref": "feature"}}
        get = lambda repo, endpoint, paginate: [pr]
        self.assertEqual(pages.select({"workflow_run": run}, "owner/repo", get), ("preview", 42))
        pr["head"]["sha"] = "newer"
        self.assertEqual(pages.select({"workflow_run": run}, "owner/repo", get), ("skip", ""))
        get = lambda repo, endpoint, paginate: []
        self.assertEqual(pages.select({"workflow_run": run}, "owner/repo", get), ("skip", ""))

    def test_failed_run_is_not_published(self):
        event = {"workflow_run": {"conclusion": "failure"}}
        self.assertEqual(pages.select(event, "owner/repo", None), ("skip", ""))

    def test_reopened_pr_is_not_cleaned_up(self):
        event = {"action": "closed", "pull_request": {"number": 42}}
        get = lambda repo, endpoint: {"state": "closed"}
        self.assertEqual(pages.select(event, "owner/repo", get), ("cleanup", 42))
        get = lambda repo, endpoint: {"state": "open"}
        self.assertEqual(pages.select(event, "owner/repo", get), ("skip", ""))


if __name__ == "__main__":
    unittest.main()
