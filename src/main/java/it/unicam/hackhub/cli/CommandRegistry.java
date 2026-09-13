package it.unicam.hackhub.cli;

import it.unicam.hackhub.model.enums.StaffRole;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class CommandRegistry {
    public enum Audience {
        GUEST,
        USER,
        STAFF
    }

    private final Map<String, CommandDescriptor> commands = new HashMap<>();

    // Registra un comando visibile a tutti i profili.
    public void register(Command command) {
        register(command, EnumSet.allOf(Audience.class), Set.of());
    }

    // Registra un comando con audience e ruoli staff richiesti.
    public void register(Command command, EnumSet<Audience> visibleFor, Set<StaffRole> requiredStaffRoles) {
        commands.put(command.name(), new CommandDescriptor(
                command,
                EnumSet.copyOf(visibleFor),
                normalizeRoles(requiredStaffRoles)
        ));
    }

    // Cerca un comando senza applicare filtri di visibilita'.
    public Optional<Command> find(String name) {
        CommandDescriptor descriptor = commands.get(name);
        if (descriptor == null) {
            return Optional.empty();
        }
        return Optional.of(descriptor.command());
    }

    // Cerca un comando ma lo restituisce solo se e' visibile al profilo corrente.
    public Optional<Command> findVisible(String name, SessionContext sessionContext) {
        CommandDescriptor descriptor = commands.get(name);
        if (descriptor == null || !isVisible(descriptor, sessionContext)) {
            return Optional.empty();
        }
        return Optional.of(descriptor.command());
    }

    public Set<String> names() {
        return new TreeSet<>(commands.keySet());
    }

    // Restituisce i soli comandi eseguibili nella sessione corrente.
    public List<String> getVisibleCommands(SessionContext sessionContext) {
        Audience audience = resolveAudience(sessionContext);
        Set<StaffRole> currentStaffRoles = sessionContext.getStaffRoles();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, CommandDescriptor> entry : commands.entrySet()) {
            CommandDescriptor descriptor = entry.getValue();
            if (isVisible(descriptor, audience, currentStaffRoles)) {
                result.add(entry.getKey());
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    private Audience resolveAudience(SessionContext sessionContext) {
        if (sessionContext.isUserLoggedIn()) {
            return Audience.USER;
        }
        if (sessionContext.isStaffLoggedIn()) {
            return Audience.STAFF;
        }
        return Audience.GUEST;
    }

    private Set<StaffRole> normalizeRoles(Set<StaffRole> requiredStaffRoles) {
        if (requiredStaffRoles == null || requiredStaffRoles.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(requiredStaffRoles);
    }

    private boolean isVisible(CommandDescriptor descriptor, SessionContext sessionContext) {
        Audience audience = resolveAudience(sessionContext);
        return isVisible(descriptor, audience, sessionContext.getStaffRoles());
    }

    private boolean isVisible(CommandDescriptor descriptor, Audience audience, Set<StaffRole> currentStaffRoles) {
        if (!descriptor.visibleFor().contains(audience)) {
            return false;
        }
        if (audience != Audience.STAFF || descriptor.requiredStaffRoles().isEmpty()) {
            return true;
        }
        return descriptor.requiredStaffRoles().stream().anyMatch(currentStaffRoles::contains);
    }

    private record CommandDescriptor(
            Command command,
            EnumSet<Audience> visibleFor,
            Set<StaffRole> requiredStaffRoles
    ) { }
}
