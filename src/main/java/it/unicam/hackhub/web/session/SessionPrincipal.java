package it.unicam.hackhub.web.session;

public final class SessionPrincipal {
    public enum ProfileType {
        USER,
        STAFF
    }

    private final ProfileType type;
    private final long id;

    public SessionPrincipal(ProfileType type, long id) {
        if (type == null) {
            throw new IllegalArgumentException("Tipo profilo non valido");
        }
        this.type = type;
        this.id = id;
    }

    public ProfileType getType() {
        return type;
    }

    public long getId() {
        return id;
    }
}
