# pi-calculator
A simple demonstration of calculating pi through random (x,y) point selection in ScalaJS

See it in action: https://tylerprete.github.io/pi-calculator/

If you'd like to check out the project and build locally:
1. Check out repository
2. Install JDK 21 or newer, then run `./build.sh` from the project root. The Mill wrapper downloads the pinned toolchain.
3. open index.html from the public/ directory in a web browser.

The build uses Scala 3.10.0, Scala.js 1.22.0, and Mill 1.3.0.

After building, run `bash ./test.sh` to check the calculator in headless Chrome.
The check covers point plotting, all step sizes, pi estimation, automatic
incrementing, stopping, and resetting. On Linux it uses `google-chrome`; on
macOS it uses the standard Google Chrome application path. Set `CHROME_BIN`
to use a different Chrome or Chromium executable.

## CI and deployment

`Scala CI` runs for pushes to `master` and pull requests targeting `master`.
It installs Liberica JDK 21, builds the optimized JavaScript bundle, runs the
browser smoke test and deployment regression tests, and uploads `public/`.
The build job uses a read-only token.

After CI succeeds, `Publish Pages` downloads the build artifact and publishes
to the existing `gh-pages` branch. Production updates replace the root site
while preserving PR previews. The deployment is skipped if the build's commit
is no longer the current `master` commit.

## Pull request previews

Successful pull request builds publish a preview at
`https://tylerprete.github.io/pi-calculator/previews/pr-N/`, where `N` is the
PR number. The build and publishing workflow summaries include this URL.
Each update replaces that PR's preview; closing or merging the PR removes it.
Production deployments preserve the previews and previews preserve production.

The publisher uses trusted `master` code and treats PR artifacts as static
files, without executing them. This also supports fork PRs once their CI run
is approved. Publishing skips outdated PR builds and closed PRs, and all
deployments and cleanup run through one queue to avoid conflicting writes.
The publishing workflow must be merged into `master` before previews activate.
GitHub Pages must continue to publish from the root of the `gh-pages` branch;
preview URLs become available after its publishing job completes.

## Deployment development

The workflows live in `.github/workflows/scala.yml` and
`.github/workflows/pages.yml`. Deployment selection and site assembly live in
`.github/scripts/Pages.scala`. The publisher validates artifacts before copying
them and rejects symlinks, hidden control files, missing `index.html`, and
builds that use the reserved `previews/` directory. Asset paths must remain
relative so the app also works under preview URLs.

The publisher and its uTest regression suite are single-file Scala scripts,
run through the repository's pinned Mill launcher. Run the checks with:

```bash
./millw --no-server tests/PagesTests.scala
```

These verify preview isolation, cleanup, stale-build selection, and artifact
validation. Use `actionlint` to check workflow changes and `git diff --check`
to check whitespace. Actionlint 1.7.12 predates `concurrency.queue`; that key
must be checked against current GitHub documentation separately.
