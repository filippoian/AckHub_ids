package it.unicam.hackhub.model.state;

import it.unicam.hackhub.model.enums.HackathonStatus;

public class RegistrationState implements HackathonState {
    @Override
    public boolean canRegister() {
        return true;
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
        return false;
    }

    @Override
    public HackathonStatus status() {
        return HackathonStatus.REGISTRATION;
    }

    @Override
    public HackathonStatus nextStatus(HackathonStatus target) {
        // Da REGISTRATION si passa solo a RUNNING.
        if (target == HackathonStatus.RUNNING) {
            return target;
        }
        throw new IllegalStateException("Invalid transition: REGISTRATION -> " + target);
    }
}
