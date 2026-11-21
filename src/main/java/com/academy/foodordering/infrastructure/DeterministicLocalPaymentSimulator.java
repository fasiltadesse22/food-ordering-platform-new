package com.academy.foodordering.infrastructure;

import com.academy.foodordering.application.simulation.SimulatedPaymentPort;
import org.springframework.stereotype.Component;

/** Pure preset-to-result teaching adapter. No HTTP, money or persistence. */
@Component
public final class DeterministicLocalPaymentSimulator implements SimulatedPaymentPort {
    @Override
    public Observation observe(Effect effect, Preset preset) {
        if (effect == null || preset == null) throw new IllegalArgumentException("Simulation preset required");
        return switch (preset) {
            case CONFIRMED -> Observation.SIMULATED_CONFIRMED;
            case DECLINED -> Observation.SIMULATED_DECLINED;
            case RESPONSE_LOST -> Observation.UNKNOWN;
        };
    }
}
