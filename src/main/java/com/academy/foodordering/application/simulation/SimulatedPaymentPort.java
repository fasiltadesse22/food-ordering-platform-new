package com.academy.foodordering.application.simulation;

/**
 * Contract for a DETERMINISTIC LOCAL TEACHING DOUBLE, not a real payment
 * provider interface or an authenticated payment callback.
 */
public interface SimulatedPaymentPort {
    enum Effect { CAPTURE, REFUND }
    enum Preset { CONFIRMED, DECLINED, RESPONSE_LOST }
    enum Observation { SIMULATED_CONFIRMED, SIMULATED_DECLINED, UNKNOWN }
    Observation observe(Effect effect, Preset preset);
}
