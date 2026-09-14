#!/usr/bin/env python3

from __future__ import annotations

import json
import os
import subprocess
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass, field
from pathlib import Path


WORKSPACE = Path(os.environ.get("GITHUB_WORKSPACE", Path.cwd())).resolve()


@dataclass
class TestFileStats:
    path: Path
    module: str
    tests: int = 0
    passed: int = 0
    failures: int = 0
    errors: int = 0
    skipped: int = 0
    duration: float = 0.0


@dataclass
class TestStats:
    files: list[TestFileStats] = field(default_factory=list)

    @property
    def tests(self) -> int:
        return sum(item.tests for item in self.files)

    @property
    def passed(self) -> int:
        return sum(item.passed for item in self.files)

    @property
    def failures(self) -> int:
        return sum(item.failures for item in self.files)

    @property
    def errors(self) -> int:
        return sum(item.errors for item in self.files)

    @property
    def skipped(self) -> int:
        return sum(item.skipped for item in self.files)

    @property
    def duration(self) -> float:
        return sum(item.duration for item in self.files)


@dataclass
class SourceCoverage:
    module: str
    package: str
    name: str
    lines: dict[int, tuple[int, int]]


@dataclass
class CoverageStats:
    reports: list[Path] = field(default_factory=list)
    sources: list[SourceCoverage] = field(default_factory=list)
    instruction_covered: int = 0
    instruction_missed: int = 0
    line_covered: int = 0
    line_missed: int = 0
    branch_covered: int = 0
    branch_missed: int = 0


@dataclass
class ChangedFile:
    path: str
    lines: set[int] = field(default_factory=set)


def local_name(tag: object) -> str:
    if not isinstance(tag, str):
        return ""
    return tag.rsplit("}", 1)[-1]


def integer(value: str | None) -> int:
    try:
        return int(value or "0")
    except ValueError:
        return 0


def decimal(value: str | None) -> float:
    try:
        return float(value or "0")
    except ValueError:
        return 0.0


def relative_path(path: Path) -> str:
    return path.relative_to(WORKSPACE).as_posix()


def module_name(path: Path) -> str:
    parts = path.relative_to(WORKSPACE).parts
    try:
        build_index = parts.index("build")
    except ValueError:
        return ":"
    if build_index == 0:
        return ":"
    return ":" + ":".join(parts[:build_index])


def parse_test_file(path: Path) -> TestFileStats:
    root = ElementTree.parse(path).getroot()
    result = TestFileStats(path=path, module=module_name(path))
    test_cases = [element for element in root.iter() if local_name(element.tag) == "testcase"]

    for test_case in test_cases:
        result.tests += 1
        if any(local_name(child.tag) == "skipped" for child in test_case):
            result.skipped += 1
        elif any(local_name(child.tag) == "failure" for child in test_case):
            result.failures += 1
        elif any(local_name(child.tag) == "error" for child in test_case):
            result.errors += 1
        else:
            result.passed += 1

    if not test_cases:
        suites = [element for element in root.iter() if local_name(element.tag) == "testsuite"]
        for suite in suites:
            result.tests += integer(suite.get("tests"))
            result.failures += integer(suite.get("failures"))
            result.errors += integer(suite.get("errors"))
            result.skipped += integer(suite.get("skipped"))
        result.passed = max(result.tests - result.failures - result.errors - result.skipped, 0)

    if local_name(root.tag) == "testsuite":
        result.duration = decimal(root.get("time"))
    else:
        result.duration = sum(
            decimal(suite.get("time"))
            for suite in root.iter()
            if local_name(suite.tag) == "testsuite"
        )
    return result


def load_test_stats() -> tuple[TestStats, list[str]]:
    stats = TestStats()
    warnings: list[str] = []
    paths = sorted(WORKSPACE.glob("**/build/test-results/test/*.xml"))
    for path in paths:
        if "build-logic" in path.parts:
            continue
        try:
            stats.files.append(parse_test_file(path))
        except (ElementTree.ParseError, OSError) as error:
            warnings.append(f"Could not parse test report {relative_path(path)}: {error}")
    return stats, warnings


def parse_coverage_file(path: Path) -> tuple[CoverageStats, list[str]]:
    root = ElementTree.parse(path).getroot()
    stats = CoverageStats(reports=[path])
    warnings: list[str] = []

    for counter in root:
        if local_name(counter.tag) == "counter":
            missed = integer(counter.get("missed"))
            covered = integer(counter.get("covered"))
            counter_type = counter.get("type")
            if counter_type == "INSTRUCTION":
                stats.instruction_missed += missed
                stats.instruction_covered += covered
            elif counter_type == "LINE":
                stats.line_missed += missed
                stats.line_covered += covered
            elif counter_type == "BRANCH":
                stats.branch_missed += missed
                stats.branch_covered += covered

    for package in root.iter():
        if local_name(package.tag) != "package":
            continue
        package_name = package.get("name", "").replace(".", "/")
        for source_file in package:
            if local_name(source_file.tag) != "sourcefile":
                continue
            lines: dict[int, tuple[int, int]] = {}
            for line in source_file:
                if local_name(line.tag) != "line":
                    continue
                line_number = integer(line.get("nr"))
                lines[line_number] = (integer(line.get("mi")), integer(line.get("ci")))
            stats.sources.append(
                SourceCoverage(
                    module=module_name(path),
                    package=package_name,
                    name=source_file.get("name", ""),
                    lines=lines,
                )
            )
    return stats, warnings


def load_coverage_stats() -> tuple[CoverageStats, list[str]]:
    result = CoverageStats()
    warnings: list[str] = []
    paths = sorted(WORKSPACE.glob("**/build/reports/jacoco/**/*.xml"))
    for path in paths:
        if "build-logic" in path.parts:
            continue
        try:
            stats, file_warnings = parse_coverage_file(path)
            result.reports.extend(stats.reports)
            result.sources.extend(stats.sources)
            result.instruction_covered += stats.instruction_covered
            result.instruction_missed += stats.instruction_missed
            result.line_covered += stats.line_covered
            result.line_missed += stats.line_missed
            result.branch_covered += stats.branch_covered
            result.branch_missed += stats.branch_missed
            warnings.extend(file_warnings)
        except (ElementTree.ParseError, OSError) as error:
            warnings.append(f"Could not parse coverage report {relative_path(path)}: {error}")
    return result, warnings


def event_payload() -> dict:
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path:
        return {}
    try:
        payload = json.loads(Path(event_path).read_text(encoding="utf-8"))
        return payload if isinstance(payload, dict) else {}
    except (OSError, json.JSONDecodeError):
        return {}


def git_revision_range() -> tuple[str | None, str]:
    event = event_payload()
    event_name = os.environ.get("GITHUB_EVENT_NAME", "")
    head = os.environ.get("GITHUB_SHA", "HEAD")
    if event_name == "pull_request":
        base = event.get("pull_request", {}).get("base", {}).get("sha")
        return base, head
    if event_name == "push":
        before = event.get("before")
        if before and not all(character == "0" for character in before):
            return before, head
    return None, head


def git_diff() -> str:
    base, head = git_revision_range()
    if base:
        command = ["git", "diff", "--unified=0", "--no-color", f"{base}...{head}", "--"]
    else:
        command = ["git", "diff-tree", "--root", "--unified=0", "--no-commit-id", "-r", head, "--"]
    try:
        result = subprocess.run(
            command,
            cwd=WORKSPACE,
            check=True,
            capture_output=True,
            text=True,
        )
        return result.stdout
    except (OSError, subprocess.CalledProcessError):
        return ""


def added_line_range(hunk: str) -> tuple[int, int] | None:
    parts = hunk.split("@@", 2)
    if len(parts) < 2:
        return None
    range_part = next((part for part in parts[1].split() if part.startswith("+")), None)
    if range_part is None:
        return None
    values = range_part[1:].split(",", 1)
    try:
        start = int(values[0])
        count = int(values[1]) if len(values) == 2 else 1
    except ValueError:
        return None
    return start, count


def changed_files() -> list[ChangedFile]:
    result: dict[str, ChangedFile] = {}
    current: ChangedFile | None = None
    for line in git_diff().splitlines():
        if line.startswith("+++ "):
            path = line[4:]
            if path == "/dev/null":
                current = None
                continue
            if path.startswith("b/"):
                path = path[2:]
            current = result.setdefault(path, ChangedFile(path))
            continue
        if current is not None and line.startswith("@@"):
            line_range = added_line_range(line)
            if line_range is None:
                continue
            start, count = line_range
            current.lines.update(range(start, start + count))
    return sorted(result.values(), key=lambda item: item.path)


def source_location(path: str) -> tuple[str, str, str] | None:
    parts = Path(path).parts
    for source_directory in (("src", "main", "java"), ("src", "main", "kotlin")):
        try:
            source_index = next(
                index
                for index in range(len(parts) - 2)
                if tuple(parts[index : index + 3]) == source_directory
            )
        except StopIteration:
            continue
        package = "/".join(parts[source_index + 3 : -1])
        module = ":" if source_index == 0 else ":" + ":".join(parts[:source_index])
        return module, package, parts[-1]
    return None


def changed_coverage(files: list[ChangedFile], coverage: CoverageStats) -> tuple[int, int, int, int]:
    covered = 0
    missed = 0
    mapped_files = 0
    unmapped_lines = 0
    for changed_file in files:
        location = source_location(changed_file.path)
        if location is None:
            continue
        module, package, name = location
        candidates = [
            source
            for source in coverage.sources
            if source.module == module and source.package == package and source.name == name
        ]
        if not candidates:
            candidates = [
                source
                for source in coverage.sources
                if source.package == package and source.name == name
            ]
        if not candidates:
            unmapped_lines += len(changed_file.lines)
            continue
        mapped_files += 1
        source = candidates[0]
        for line_number in changed_file.lines:
            line_coverage = source.lines.get(line_number)
            if line_coverage is None:
                unmapped_lines += 1
                continue
            missed_instructions, covered_instructions = line_coverage
            if missed_instructions + covered_instructions == 0:
                unmapped_lines += 1
            elif covered_instructions > 0:
                covered += 1
            else:
                missed += 1
    return covered, missed, mapped_files, unmapped_lines


def percentage(covered: int, missed: int) -> str:
    total = covered + missed
    if total == 0:
        return "N/A"
    return f"{covered / total * 100:.1f}% ({covered}/{total})"


def percentage_value(covered: int, missed: int) -> str:
    total = covered + missed
    if total == 0:
        return "N/A"
    return f"{covered / total * 100:.1f}%"


def format_duration(seconds: float) -> str:
    return f"{seconds:.2f}s"


def module_test_stats(stats: TestStats) -> list[tuple[str, TestStats]]:
    grouped: dict[str, TestStats] = {}
    for item in stats.files:
        module_stats = grouped.setdefault(item.module, TestStats())
        module_stats.files.append(item)
    return sorted(grouped.items())


def list_section(title: str, paths: list[str]) -> list[str]:
    lines = [f"<details>", f"<summary>{title} ({len(paths)})</summary>", ""]
    if paths:
        lines.extend(f"- `{path}`" for path in paths)
    else:
        lines.append("- None")
    lines.extend(["", "</details>", ""])
    return lines


def build_summary(
    tests: TestStats,
    coverage: CoverageStats,
    changed: list[ChangedFile],
    warnings: list[str],
) -> list[str]:
    raw_build_result = os.environ.get("BUILD_RESULT", "unknown").lower()
    build_result = {
        "success": "PASSED",
        "failure": "FAILED",
        "cancelled": "CANCELLED",
        "skipped": "SKIPPED",
    }.get(raw_build_result, raw_build_result.replace("_", " ").upper())
    changed_covered, changed_missed, mapped_files, unmapped_lines = changed_coverage(changed, coverage)
    module_rows = module_test_stats(tests)
    test_rate = percentage_value(tests.passed, tests.tests - tests.passed)
    line_rate = percentage_value(coverage.line_covered, coverage.line_missed)
    changed_line_rate = percentage_value(changed_covered, changed_missed)
    jar_files = sorted(
        relative_path(path)
        for path in WORKSPACE.glob("**/build/libs/*.jar")
        if "build-logic" not in path.parts
    )
    test_files = [relative_path(item.path) for item in tests.files]
    coverage_files = sorted(relative_path(path) for path in coverage.reports)
    coverage_html = sorted(
        relative_path(path)
        for path in WORKSPACE.glob("**/build/reports/jacoco/**/index.html")
        if "build-logic" not in path.parts
    )
    changed_paths = [item.path for item in changed]
    lines = [
        "## Verification Summary",
        "",
        "| Build | Tests | Pass rate | Line coverage | Changed line coverage |",
        "| --- | ---: | ---: | ---: | ---: |",
        f"| **{build_result}** | **{tests.passed} / {tests.tests} passed** | **{test_rate}** | "
        f"**{line_rate}** | **{changed_line_rate}** |",
        "",
        "### Test Results",
        "",
        "| Total | Passed | Failed | Errors | Skipped | Duration | Reports |",
        "| ---: | ---: | ---: | ---: | ---: | ---: | ---: |",
        f"| {tests.tests} | {tests.passed} | {tests.failures} | {tests.errors} | "
        f"{tests.skipped} | {format_duration(tests.duration)} | {len(tests.files)} |",
        "",
        "<details>",
        f"<summary>Module test breakdown ({len(module_rows)})</summary>",
        "",
        "| Module | Tests | Passed | Failed | Errors | Skipped | Pass rate |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: |",
    ]
    if module_rows:
        for module, module_stats in module_rows:
            failed_total = module_stats.failures + module_stats.errors
            lines.append(
                f"| `{module}` | {module_stats.tests} | {module_stats.passed} | "
                f"{module_stats.failures} | {module_stats.errors} | {module_stats.skipped} | "
                f"{percentage_value(module_stats.passed, failed_total + module_stats.skipped)} |"
            )
    else:
        lines.append("| None | 0 | 0 | 0 | 0 | 0 | N/A |")
    lines.extend(["", "</details>", ""])

    lines.extend(
        [
            "### Coverage",
            "",
            "| Scope | Instructions | Lines | Branches |",
            "| --- | ---: | ---: | ---: |",
            f"| All modules | {percentage(coverage.instruction_covered, coverage.instruction_missed)} | "
            f"{percentage(coverage.line_covered, coverage.line_missed)} | "
            f"{percentage(coverage.branch_covered, coverage.branch_missed)} |",
            f"| Changed executable lines | N/A | {percentage(changed_covered, changed_missed)} | N/A |",
            "",
            f"Changed source files mapped: `{mapped_files}`; unmapped changed lines: `{unmapped_lines}`.",
            "",
            "### Files",
            "",
        ]
    )
    lines.extend(list_section("Changed files", changed_paths))
    lines.extend(list_section("Built JAR files", jar_files))
    lines.extend(list_section("JUnit XML reports", test_files))
    lines.extend(list_section("JaCoCo XML reports", coverage_files))
    lines.extend(list_section("JaCoCo HTML report entry points", coverage_html))

    if warnings:
        lines.extend(["### Metrics Warnings", ""])
        lines.extend(f"- {warning}" for warning in warnings)
        lines.append("")
    return lines


def write_manifest(
    path: Path,
    tests: TestStats,
    coverage: CoverageStats,
    changed: list[ChangedFile],
) -> None:
    jar_files = sorted(
        relative_path(item)
        for item in WORKSPACE.glob("**/build/libs/*.jar")
        if "build-logic" not in item.parts
    )
    lines = [
        "Changed files",
        *[item.path for item in changed],
        "",
        "Built JAR files",
        *jar_files,
        "",
        "JUnit XML reports",
        *[relative_path(item.path) for item in tests.files],
        "",
        "JaCoCo XML reports",
        *[relative_path(item) for item in coverage.reports],
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    tests, test_warnings = load_test_stats()
    coverage, coverage_warnings = load_coverage_stats()
    changed = changed_files()
    warnings = test_warnings + coverage_warnings
    summary = build_summary(tests, coverage, changed, warnings)
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with Path(summary_path).open("a", encoding="utf-8") as file:
            file.write("\n".join(summary) + "\n")
    else:
        print("\n".join(summary))

    manifest_path = Path(os.environ.get("RUNNER_TEMP", "/tmp")) / "luxspec-build-files.txt"
    write_manifest(manifest_path, tests, coverage, changed)


if __name__ == "__main__":
    main()
