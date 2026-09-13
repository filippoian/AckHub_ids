package it.unicam.hackhub.controller;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.Invitation;
import it.unicam.hackhub.model.Team;
import it.unicam.hackhub.model.TeamRegistration;
import it.unicam.hackhub.model.User;
import it.unicam.hackhub.model.enums.InvitationStatus;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.InvitationRepository;
import it.unicam.hackhub.repository.TeamRegistrationRepository;
import it.unicam.hackhub.repository.TeamRepository;
import it.unicam.hackhub.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TeamController {
    private final TeamRepository teamRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final HackathonRepository hackathonRepository;

    public TeamController(TeamRepository teamRepository,
                          InvitationRepository invitationRepository,
                          UserRepository userRepository,
                          TeamRegistrationRepository teamRegistrationRepository,
                          HackathonRepository hackathonRepository) {
        this.teamRepository = teamRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.teamRegistrationRepository = teamRegistrationRepository;
        this.hackathonRepository = hackathonRepository;
    }

    // Crea un team nuovo e collega subito il creator.
    @org.springframework.transaction.annotation.Transactional
    public Team createTeam(long creatorUserId, String teamName) {
        User creator = userRepository.findById(creatorUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found"));
        // Un utente puo' stare in un solo team.
        if (creator.getTeamId() != null) {
            throw new IllegalStateException("User already belongs to a team");
        }
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("Nome team non valido");
        }
        String normalizedName = teamName.trim();
        if (teamRepository.findByName(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Nome team gia in uso");
        }

        Team team = teamRepository.save(new Team(0L, normalizedName));
        creator.setTeamId(team.getTeamId());
        userRepository.save(creator);
        return team;
    }

    // Invito singolo e multiplo condividono identici controlli sul chiamante.
    @org.springframework.transaction.annotation.Transactional
    public Invitation inviteUser(long currentUserId, long teamId, long invitedUserId) {
        return inviteUsers(currentUserId, teamId, List.of(invitedUserId)).get(0);
    }

    @org.springframework.transaction.annotation.Transactional
    public List<Invitation> inviteUsers(long currentUserId, long teamId, List<Long> invitedUserIds) {
        requireMember(currentUserId, teamId);
        assertTeamModifiable(teamId);
        if (invitedUserIds == null || invitedUserIds.isEmpty()
                || invitedUserIds.stream().anyMatch(id -> id == null || id <= 0)
                || new java.util.HashSet<>(invitedUserIds).size() != invitedUserIds.size()) {
            throw new IllegalArgumentException("Selezionare destinatari distinti e validi");
        }
        assertCapacity(teamId, invitedUserIds.size());
        // L'intero elenco viene validato prima del primo salvataggio.
        for (long userId : invitedUserIds) {
            User invited = userRepository.findById(userId).filter(User::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("Destinatario non trovato"));
            if (invited.getTeamId() != null) throw new IllegalStateException("Destinatario già in un team");
            if (invitationRepository.findPendingByTeamAndUser(teamId, userId).isPresent()) {
                throw new IllegalStateException("Invito pendente già presente");
            }
        }
        return invitedUserIds.stream().map(userId -> invitationRepository.save(
                new Invitation(0, teamId, userId, InvitationStatus.PENDING))).toList();
    }

    public List<Invitation> viewSentInvites(long currentUserId) {
        Long teamId = getTeamIdOfUser(currentUserId);
        if (teamId == null) throw new IllegalStateException("Non appartieni a un team");
        requireMember(currentUserId, teamId);
        return invitationRepository.findByTeamId(teamId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void cancelInvitation(long currentUserId, long invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invito non trovato"));
        requireMember(currentUserId, invitation.getTeamId());
        if (invitation.getStatus() != InvitationStatus.PENDING) throw new IllegalStateException("Invito già gestito");
        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
    }

    @org.springframework.transaction.annotation.Transactional
    public void leaveTeam(long currentUserId) {
        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        if (user.getTeamId() == null) throw new IllegalStateException("Non appartieni a un team");
        long teamId = user.getTeamId();
        assertTeamModifiable(teamId);
        boolean lastMember = userRepository.findByTeamId(teamId).size() == 1;
        user.setTeamId(null);
        userRepository.save(user);
        if (lastMember) {
            for (Invitation invitation : invitationRepository.findByTeamId(teamId)) {
                if (invitation.getStatus() == InvitationStatus.PENDING) {
                    invitation.setStatus(InvitationStatus.CANCELLED);
                    invitationRepository.save(invitation);
                }
            }
        }
    }

    private void requireMember(long currentUserId, long teamId) {
        User member = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        if (member.getTeamId() == null || member.getTeamId() != teamId) {
            throw new IllegalArgumentException("Operazione consentita solo ai membri del team");
        }
        if (teamRepository.findById(teamId).isEmpty()) throw new IllegalArgumentException("Team non trovato");
    }

    private void assertTeamModifiable(long teamId) {
        if (teamRepository.findById(teamId).isEmpty()) throw new IllegalArgumentException("Team non trovato");
        teamRegistrationRepository.findByTeamId(teamId).ifPresent(reg -> {
            Hackathon event = hackathonRepository.findById(reg.getHackathonId()).orElseThrow();
            if (event.getStatus() == it.unicam.hackhub.model.enums.HackathonStatus.RUNNING
                    || event.getStatus() == it.unicam.hackhub.model.enums.HackathonStatus.REVIEW) {
                throw new IllegalStateException("Composizione bloccata durante gara e valutazione");
            }
            if (reg.isExpelled() && !event.isClosed()) throw new IllegalStateException("Team espulso");
        });
    }

    private void assertCapacity(long teamId, int additionalMembers) {
        teamRegistrationRepository.findByTeamId(teamId).ifPresent(reg -> {
            Hackathon event = hackathonRepository.findById(reg.getHackathonId()).orElseThrow();
            if (!event.isClosed() && (long) userRepository.findByTeamId(teamId).size() + additionalMembers > event.getMaxTeamSize()) {
                throw new IllegalStateException("Dimensione massima del team superata");
            }
        });
    }

    // Restituisce il team dell'utente loggato.
    public Long getTeamIdOfUser(long userId) {
        User user = userRepository.findById(userId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getTeamId();
    }

    // Elenca gli inviti dell'utente senza informazioni extra.
    public List<Invitation> viewInvites(long userId) {
        if (userRepository.findById(userId).filter(User::isActive).isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        return invitationRepository.findByInvitedUserId(userId);
    }

    // Elenca gli inviti con nome team, numero membri e hackathon collegato.
    public List<InvitationView> viewInvitesForUser(long userId) {
        if (userRepository.findById(userId).filter(User::isActive).isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        Map<Long, Integer> memberCountByTeam = buildMemberCountByTeam();
        return invitationRepository.findByInvitedUserId(userId).stream()
                .map(invitation -> {
                    long teamId = invitation.getTeamId();
                    String teamName = teamRepository.findById(teamId)
                            .map(Team::getTeamName)
                            .orElse("N/A");
                    String hackathonLabel = teamRegistrationRepository.findByTeamId(teamId)
                            .map(TeamRegistration::getHackathonId)
                            .map(hackathonId -> hackathonRepository.findById(hackathonId)
                                    .map(Hackathon::getHackathonName)
                                    .orElse("ID:" + hackathonId))
                            .orElse("N/A");

                    return new InvitationView(
                            invitation.getInvitationId(),
                            teamId,
                            teamName,
                            memberCountByTeam.getOrDefault(teamId, 0),
                            hackathonLabel,
                            String.valueOf(invitation.getStatus())
                    );
                })
                .toList();
    }

    // Accetta l'invito solo se e' ancora valido e coerente con i vincoli del team.
    @org.springframework.transaction.annotation.Transactional
    public boolean acceptInvitation(long invitationId, long currentUserId) {
        Optional<Invitation> invitationOpt = invitationRepository.findById(invitationId);
        if (invitationOpt.isEmpty()) {
            return false;
        }

        Invitation invitation = invitationOpt.get();
        // Non si puo' accettare due volte lo stesso invito.
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            return false;
        }
        // L'invito puo' essere gestito solo dal destinatario.
        if (invitation.getInvitedUserId() != currentUserId) {
            return false;
        }

        Optional<User> invitedUserOpt = userRepository.findById(invitation.getInvitedUserId());
        if (invitedUserOpt.isEmpty()) {
            return false;
        }

        User invitedUser = invitedUserOpt.get();
        if (invitedUser.getTeamId() != null) {
            return false;
        }

        assertTeamModifiable(invitation.getTeamId());
        assertCapacity(invitation.getTeamId(), 1);

        invitedUser.setTeamId(invitation.getTeamId());
        userRepository.save(invitedUser);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
        return true;
    }

    // Rifiuta un invito pendente del destinatario corrente.
    @org.springframework.transaction.annotation.Transactional
    public boolean declineInvitation(long invitationId, long currentUserId) {
        Optional<Invitation> invitationOpt = invitationRepository.findById(invitationId);
        if (invitationOpt.isEmpty()) {
            return false;
        }

        Invitation invitation = invitationOpt.get();
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            return false;
        }
        if (invitation.getInvitedUserId() != currentUserId) {
            return false;
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);
        return true;
    }

    // Helper usato per mostrare il numero membri nei riepiloghi inviti.
    private Map<Long, Integer> buildMemberCountByTeam() {
        Map<Long, Integer> counts = new HashMap<>();
        for (User user : userRepository.findAll()) {
            Long teamId = user.getTeamId();
            if (teamId == null) {
                continue;
            }
            counts.put(teamId, counts.getOrDefault(teamId, 0) + 1);
        }
        return counts;
    }

    public record InvitationView(long invitationId,
                                 long teamId,
                                 String teamName,
                                 int members,
                                 String hackathon,
                                 String status) {
    }
}
