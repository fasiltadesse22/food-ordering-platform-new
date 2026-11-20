package com.academy.foodordering.domain;

import java.util.Objects;

/** One deterministic, explainable result of a PURE teaching-model guard. */
public record GuardCheck(String code, boolean passed, String explanation) {
    public GuardCheck {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(explanation, "explanation");
        if (code.isBlank() || explanation.isBlank()) throw new IllegalArgumentException("Guard labels must be nonblank");
    }
}
