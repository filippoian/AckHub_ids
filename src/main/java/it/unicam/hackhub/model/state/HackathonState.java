package it.unicam.hackhub.model.state;

import it.unicam.hackhub.model.enums.HackathonStatus;

public interface HackathonState {
    // In questa fase sono ancora aperte le iscrizioni?
    boolean canRegister();

    // In questa fase il team puo' inviare submission?
    boolean canSubmit();

    // In questa fase i judge possono valutare?
    boolean canEvaluate();

    // In questa fase l'hackathon e' gia' chiuso?
    boolean isClosed();

    // Stato rappresentato da questa implementazione.
    HackathonStatus status();

    // Restituisce la prossima transizione valida o fallisce.
    HackathonStatus nextStatus(HackathonStatus target);
}
