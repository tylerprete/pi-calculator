"""Select trusted deployment destinations and assemble the complete Pages tree.

PR artifacts are static data: never import or execute anything from them.
"""

import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys


def api(repository, endpoint, paginate=False):
    command = ["gh", "api", f"repos/{repository}/{endpoint}"]
    if paginate:
        command += ["--paginate", "--slurp"]
    result = json.loads(subprocess.check_output(command, text=True))
    return [item for page in result for item in page] if paginate else result


def select(event, repository, get):
    if "pull_request" in event:
        number = event["pull_request"]["number"]
        pr = get(repository, f"pulls/{number}")
        if event["action"] == "closed" and pr["state"] == "closed":
            return "cleanup", number
        return "skip", ""

    run = event["workflow_run"]
    if run["conclusion"] != "success" or run["path"] != ".github/workflows/scala.yml":
        return "skip", ""
    if run["event"] == "push":
        if run["head_branch"] == "master" and run["head_sha"] == get(repository, "commits/master")["sha"]:
            return "production", ""
    elif run["event"] == "pull_request":
        # Fork runs can have an empty workflow_run.pull_requests list.
        # Look up open PRs and match their current head rather than trusting
        # a PR number or destination supplied inside an untrusted artifact.
        prs = get(repository, "pulls?state=open&base=master&per_page=100", paginate=True)
        matches = [pr for pr in prs if pr["head"]["sha"] == run["head_sha"]
                   and pr["head"]["ref"] == run["head_branch"]]
        if len(matches) == 1:
            return "preview", matches[0]["number"]
    return "skip", ""


def validate_tree(directory, checkout=False):
    """Reject links and Git control files before the deploy action copies data."""
    if directory.is_symlink() or not directory.is_dir():
        raise ValueError(f"Not a directory: {directory}")
    for entry in directory.iterdir():
        if checkout and entry.name == ".git":
            continue
        if entry.is_symlink():
            raise ValueError(f"Symlink in site: {entry}")
        if entry.name.startswith(".") and entry.name != ".nojekyll":
            raise ValueError(f"Hidden control file in site: {entry}")
        if entry.is_dir():
            validate_tree(entry)
        elif not entry.is_file():
            raise ValueError(f"Not a regular file: {entry}")


def remove(path):
    if path.is_dir():
        shutil.rmtree(path)
    elif path.exists():
        path.unlink()


def prepare(mode, number, existing, built, output):
    if mode not in {"production", "preview", "cleanup"}:
        raise ValueError(f"Invalid mode: {mode}")
    if mode != "production" and not re.fullmatch(r"[1-9][0-9]*", str(number)):
        raise ValueError("Invalid PR number")
    validate_tree(existing, checkout=True)
    if mode != "cleanup":
        validate_tree(built)
        if not (built / "index.html").is_file():
            raise ValueError("Build is missing index.html")
        if (built / "previews").exists():
            raise ValueError("Build uses reserved previews directory")

    # Start with the live tree, so previews never replace production files.
    shutil.copytree(existing, output, ignore=shutil.ignore_patterns(".git"))
    if mode == "production":
        for entry in output.iterdir():
            if entry.name != "previews":
                remove(entry)
        shutil.copytree(built, output, dirs_exist_ok=True)
    else:
        destination = output / "previews" / f"pr-{number}"
        remove(destination)
        if mode == "preview":
            shutil.copytree(built, destination)
    (output / ".nojekyll").touch()


def main():
    if sys.argv[1] == "select":
        event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
        mode, number = select(event, os.environ["GITHUB_REPOSITORY"], api)
        with open(os.environ["GITHUB_OUTPUT"], "a") as output:
            output.write(f"mode={mode}\nnumber={number}\n")
        print(f"Deployment: {mode}" + (f" for PR #{number}" if number else ""))
    elif sys.argv[1] == "prepare":
        prepare(os.environ["MODE"], os.environ.get("PR_NUMBER", ""),
                Path("existing-site"), Path("built-public"), Path("published-site"))
    else:
        raise ValueError("Expected select or prepare")


if __name__ == "__main__":
    main()
