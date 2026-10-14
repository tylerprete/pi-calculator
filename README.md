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

Note:
I have this repository auto-wired to build with github actions and auto-deploy updates to the gh-pages branch, updating the site you see above. I'm pretty happy with how it turned out!

Successful pull request builds publish a preview at
`https://tylerprete.github.io/pi-calculator/previews/pr-N/`, where `N` is the
PR number. The build and publishing workflow summaries include this URL.
Each update replaces that PR's preview; closing or merging the PR removes it.
Production deployments preserve the previews and previews preserve production.

`Scala CI` builds and tests with a read-only token. `Publish Pages` runs from
the trusted `master` branch after CI succeeds, downloads the static build
artifact, and publishes it without executing PR code. This also supports fork
PRs once their CI run is approved. Publishing skips outdated builds and closed
PRs, and all deployments and cleanup run through one queue.
The publishing workflow must be merged into `master` before previews activate.
GitHub Pages must continue to publish from the root of the `gh-pages` branch;
preview URLs become available after its publishing job completes.

Run `python3 -m unittest discover -s tests -p test_pages.py` to verify preview
isolation, cleanup, stale-build selection, and artifact validation locally.
