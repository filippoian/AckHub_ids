package it.unicam.hackhub.model.state;

import it.unicam.hackhub.model.enums.HackathonStatus;

public class ReviewState implements HackathonState {
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
        return true;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public HackathonStatus status() {
        return HackathonStatus.REVIEW;
    }

    @Override
    public HackathonStatus nextStatus(HackathonStatus target) {
        // Da REVIEW si puo' solo chiudere.
        if (target == HackathonStatus.CLOSED) {
            return target;
        }
        throw new IllegalStateException("Invalid transition: REVIEW -> " + target);
    }
}
