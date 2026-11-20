"""Regression tests for the offline, intentionally limited governance script."""
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_repository_governance as governance


class RepositoryGovernanceTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.write("README.md", (
            "# Test\n"
            "[State](docs/architecture/CURRENT_STATE.md)\n"
            "[Startup](docs/governance/SESSION_STARTUP.md)\n"
            "[Contract](docs/engineering/LESSON_1_1_7_GUARDED_TRANSITION_CONTRACT.md)\n"
        ))
        self.write("docs/architecture/CURRENT_STATE.md",
                   governance.POLICY_URL + "\n[Failure proof](../verification/FAILURE_EVIDENCE_MATRIX.md)\n")
        self.write("docs/governance/SESSION_STARTUP.md", "# Session startup\n")
        self.write("docs/governance/DOCUMENT_OWNERSHIP.md", "# Document ownership\n")
        self.write("docs/verification/FAILURE_EVIDENCE_MATRIX.md", "# Failure evidence\n")
        self.write("docs/engineering/LESSON_1_1_7_GUARDED_TRANSITION_CONTRACT.md", "# Technical rules\n")
        self.write("checkpoints/LESSON_1_1_7.md",
                   "# Lesson\n**Status:** completed\nBaseline; merge; CI; tests; limitations; next dependency.\n")

    def write(self, path, value):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(value, encoding="utf-8")

    def test_valid_repository_and_historical_candidate_are_accepted(self):
        checkpoint = self.root / "checkpoints/LESSON_1_1_7.md"
        checkpoint.write_text(checkpoint.read_text() +
                              "\n## HISTORICAL candidate log\nAt the time, merge pending; CI pending.\n")
        self.assertEqual([], governance.check(self.root))

    def test_duplicate_checkpoint_and_contract_fails(self):
        source = (self.root / "checkpoints/LESSON_1_1_7.md").read_text()
        self.write("docs/engineering/LESSON_1_1_7_GUARDED_TRANSITION_CONTRACT.md", source)
        self.assertTrue(any("exact duplicate" in m for m in governance.check(self.root)))

    def test_broken_relative_link_fails(self):
        self.write("README.md", (self.root / "README.md").read_text() + "[Broken](docs/no-such-doc.md)\n")
        self.assertTrue(any("missing local link" in m for m in governance.check(self.root)))

    def test_contradictory_active_status_fails_but_historical_log_does_not(self):
        file = self.root / "checkpoints/LESSON_1_1_7.md"
        file.write_text(file.read_text().replace("**Status:** completed",
                                                 "**Status:** completed, merge pending"))
        self.assertTrue(any("contradictory current header status" in m
                            for m in governance.check(self.root)))

    def test_unapproved_policy_reference_is_required(self):
        self.write("docs/architecture/CURRENT_STATE.md", "# Current without policy pointer\n")
        self.assertTrue(any("POL-01" in m for m in governance.check(self.root)))

    def test_domain_to_spring_import_is_flagged_not_all_source(self):
        base = "src/main/java/com/academy/foodordering/domain/Probe.java"
        self.write(base, "package com.academy.foodordering.domain;\n"
                         "import org.springframework.stereotype.Service;\n")
        self.assertTrue(any("forbidden domain import" in m
                            for m in governance.check(self.root)))
        self.write(base, "package com.academy.foodordering.domain;\nimport java.util.UUID;\n")
        self.assertFalse(any("forbidden domain import" in m
                             for m in governance.check(self.root)))


if __name__ == "__main__":
    unittest.main()
