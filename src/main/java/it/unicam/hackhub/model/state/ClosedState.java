package it.unicam.hackhub.model.state;

import it.unicam.hackhub.model.enums.HackathonStatus;

public class ClosedState implements HackathonState {
    @Override
    public boolean canRegister() {
        return false;
    }

    @Override
    public boolean canSubmit() {
        return false;
    }

    @Override
    public boolean canEvaluate() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    @Override
    public HackathonStatus status() {
        return HackathonStatus.CLOSED;
    }

    @Override
    public HackathonStatus nextStatus(HackathonStatus target) {
        // CLOSED e' terminale: nessuna transizione successiva.
        throw new IllegalStateException("Invalid transition: CLOSED -> " + target);
    }
}
