#!/usr/bin/env python3
"""Offline, intentionally narrow, repository-governance checks.

This script tests only observable local properties. It does NOT assert business
policy approval, GitHub branch protection, remote link health or runtime safety.
"""
from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parent.parent
MD_LINK = re.compile(r'(?<!!)\[[^\]]+\]\(([^\s)]+)(?:\s+["\'][^"\']+["\'])?\)')
MARKDOWN_ROOTS = ("README.md", "docs", "checkpoints")
POLICY_URL = "distributed-systems-engineering-academy/blob/main/docs/domain/CLUSTER_1_1_LESSON_1_1_5_DECISION_BACKLOG.md"


def markdown_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for entry in MARKDOWN_ROOTS:
        start = root / entry
        if start.is_file():
            files.append(start)
        elif start.is_dir():
            files.extend(sorted(start.rglob("*.md")))
    return files


def local_link_errors(root: Path) -> list[str]:
    errors: list[str] = []
    for document in markdown_files(root):
        in_fence = False
        for n, line in enumerate(document.read_text(encoding="utf-8").splitlines(), 1):
            if re.match(r'^\s*(?:```|~~~)', line):
                in_fence = not in_fence
                continue
            if in_fence:
                continue
            for match in MD_LINK.finditer(line):
                raw = match.group(1)
                if raw.startswith(("https://", "http://", "mailto:", "data:", "#", "sandbox:", "javascript:")):
                    continue
                target = unquote(raw.split("#", 1)[0].split("?", 1)[0])
                if not target:  # same-document anchor
                    continue
                resolved = (root / target.lstrip("/")) if target.startswith("/") else (document.parent / target)
                if not resolved.exists():
                    errors.append(f"{document.relative_to(root)}:{n}: missing local link {raw}")
    return errors


def duplicate_contract_errors(root: Path) -> list[str]:
    checkpoints = root / "checkpoints"
    engineering = root / "docs" / "engineering"
    if not checkpoints.exists() or not engineering.exists():
        return []
    seen: dict[str, Path] = {}
    errors: list[str] = []
    for folder in (checkpoints, engineering):
        for file in sorted(folder.glob("*.md")):
            digest = hashlib.sha256(file.read_bytes()).hexdigest()
            if digest in seen and seen[digest].parent != file.parent:
                errors.append(f"exact duplicate checkpoint/engineering documents: {seen[digest].relative_to(root)} vs {file.relative_to(root)}")
            else:
                seen[digest] = file
    return errors


def header_status_errors(root: Path) -> list[str]:
    # Historical narration is explicitly out-of-scope. Only *header* current
    # status fields are checked, to avoid false positives on old candidate logs.
    errors: list[str] = []
    for file in sorted((root / "checkpoints").glob("LESSON_*.md")) if (root / "checkpoints").exists() else []:
        head = file.read_text(encoding="utf-8").split("##", 1)[0]
        for line in head.splitlines():
            if re.search(r'^(?:\*\*)?(?:Record )?Status(?:\*\*)?\s*:', line, re.I):
                low = line.lower()
                complete = re.search(r'\b(merged|completed|complete|verified)\b', low)
                pending = re.search(r'\b(merge pending|not started|in progress|unverified|ci pending)\b', low)
                if complete and pending:
                    errors.append(f"{file.relative_to(root)}: contradictory current header status: {line.strip()}")
    return errors


def architecture_import_errors(root: Path) -> list[str]:
    errors: list[str] = []
    base = root / "src" / "main" / "java" / "com" / "academy" / "foodordering"
    prohibited = {
        "domain": re.compile(r'^import\s+(?:org\.springframework\.|jakarta\.persistence\.|com\.academy\.foodordering\.(?:api|application|infrastructure)\.)'),
        "application": re.compile(r'^import\s+com\.academy\.foodordering\.(?:api|infrastructure)\.'),
    }
    for layer, pattern in prohibited.items():
        folder = base / layer
        if not folder.exists():
            continue
        for source in sorted(folder.rglob("*.java")):
            for n, line in enumerate(source.read_text(encoding="utf-8").splitlines(), 1):
                if pattern.search(line.strip()):
                    errors.append(f"{source.relative_to(root)}:{n}: forbidden {layer} import: {line.strip()}")
    # Static source import guard, NOT a whole-program or runtime dependency audit.
    return errors


def canonical_errors(root: Path) -> list[str]:
    required = [
        "README.md", "docs/architecture/CURRENT_STATE.md",
        "docs/governance/DOCUMENT_OWNERSHIP.md",
        "docs/governance/SESSION_STARTUP.md",
        "docs/verification/FAILURE_EVIDENCE_MATRIX.md",
        "docs/engineering/LESSON_1_1_7_GUARDED_TRANSITION_CONTRACT.md",
        "checkpoints/LESSON_1_1_7.md"
    ]
    errors = [f"missing required governance reference: {p}" for p in required if not (root / p).is_file()]
    readme = (root / "README.md").read_text(encoding="utf-8") if (root / "README.md").is_file() else ""
    current = (root / "docs/architecture/CURRENT_STATE.md").read_text(encoding="utf-8") if (root / "docs/architecture/CURRENT_STATE.md").is_file() else ""
    checkpoint = (root / "checkpoints/LESSON_1_1_7.md").read_text(encoding="utf-8") if (root / "checkpoints/LESSON_1_1_7.md").is_file() else ""
    for required_ref in ("docs/architecture/CURRENT_STATE.md", "docs/governance/SESSION_STARTUP.md"):
        if required_ref not in readme:
            errors.append(f"README lacks canonical entry link: {required_ref}")
    if POLICY_URL not in current:
        errors.append("CURRENT_STATE must reference the authoritative academy POL-01–POL-10 backlog")
    for term in ("baseline", "merge", "test", "CI", "limitations", "next dependency"):
        if term.casefold() not in checkpoint.casefold():
            errors.append(f"Latest historical checkpoint missing required evidence field: {term}")
    # We do NOT guess whether an immutable SHA is GitHub's latest; that requires
    # a live read and is part of the mandatory session preflight.
    return errors


def check(root: Path) -> list[str]:
    return (
        canonical_errors(root)
        + duplicate_contract_errors(root)
        + local_link_errors(root)
        + header_status_errors(root)
        + architecture_import_errors(root)
    )


if __name__ == "__main__":
    result = check(ROOT)
    if result:
        print("Repository governance validation FAILED", file=sys.stderr)
        for finding in result:
            print(" - " + finding, file=sys.stderr)
        sys.exit(1)
    print("Repository governance validation PASSED: local references, document boundaries, canonical entrypoints, checkpoint header and limited import rules.")
