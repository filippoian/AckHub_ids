package it.unicam.hackhub.model.state;

import it.unicam.hackhub.model.enums.HackathonStatus;

public class RunningState implements HackathonState {
    @Override
    public boolean canRegister() {
        return false;
    }

    @Override
    public boolean canSubmit() {
        return true;
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
        return HackathonStatus.RUNNING;
    }

    @Override
    public HackathonStatus nextStatus(HackathonStatus target) {
        // Da RUNNING si passa solo a REVIEW.
        if (target == HackathonStatus.REVIEW) {
            return target;
        }
        throw new IllegalStateException("Invalid transition: RUNNING -> " + target);
    }
}
