package it.unicam.hackhub.controller;

import it.unicam.hackhub.controller.auth.LoginResult;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.StaffMember;
import it.unicam.hackhub.model.User;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.StaffMemberRepository;
import it.unicam.hackhub.repository.UserRepository;
import it.unicam.hackhub.security.PasswordHasher;

import java.util.EnumSet;
import java.util.List;
import it.unicam.hackhub.repository.InvitationRepository;
import it.unicam.hackhub.security.SessionInvalidator;
import it.unicam.hackhub.security.RecoveryCodeGenerator;
import it.unicam.hackhub.model.enums.InvitationStatus;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthController {
    private final UserRepository userRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;

    private final List<SessionInvalidator> sessionInvalidators;
    private final TeamController teamController;
    private final InvitationRepository invitationRepository;
    private final RecoveryCodeGenerator recoveryCodeGenerator;

    public AuthController(UserRepository userRepository,
                          StaffMemberRepository staffMemberRepository,
                          StaffAssignmentRepository staffAssignmentRepository,
                          List<SessionInvalidator> sessionInvalidators,
                          TeamController teamController,
                          InvitationRepository invitationRepository,
                          RecoveryCodeGenerator recoveryCodeGenerator) {
        this.sessionInvalidators = new java.util.concurrent.CopyOnWriteArrayList<>(sessionInvalidators);
        this.teamController = teamController;
        this.invitationRepository = invitationRepository;
        this.recoveryCodeGenerator = recoveryCodeGenerator;
        this.userRepository = userRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
    }

    // La CLI nasce dopo i servizi applicativi e registra qui la propria sessione.
    public void registerSessionInvalidator(SessionInvalidator invalidator) {
        if (!sessionInvalidators.contains(invalidator)) sessionInvalidators.add(invalidator);
    }

    // Gestisce il login scegliendo il flusso USER o STAFF.
    public LoginResult login(String loginType, String identifier, String password) {
        if (isBlank(loginType) || isBlank(identifier) || isBlank(password)) {
            return LoginResult.invalidInput();
        }

        // USER e STAFF hanno archivi diversi, quindi qui separiamo subito i due flussi.
        if ("USER".equalsIgnoreCase(loginType)) {
            return loginUser(identifier, password);
        }
        if ("STAFF".equalsIgnoreCase(loginType)) {
            return loginStaff(identifier, password);
        }
        return LoginResult.invalidLoginType();
    }

    @Transactional
    public synchronized RegistrationResult registerUser(String userName, String password) {
        if (isBlank(userName) || isBlank(password)) throw new IllegalArgumentException("Username e password obbligatori");
        String name = userName.trim();
        if (userRepository.findByUserName(name).isPresent()) throw new IllegalStateException("Username già utilizzato");
        String code = recoveryCodeGenerator.generate();
        User user = new User(0L, name, PasswordHasher.hashPassword(password), null);
        user.setRecoveryCodeHash(PasswordHasher.hashPassword(code));
        User saved = userRepository.save(user);
        return new RegistrationResult(new UserProfile(saved.getUserId(), saved.getUserName(), saved.getEmail(), saved.getTeamId()), code);
    }

    public UserProfile viewProfile(long currentUserId) {
        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Account non disponibile"));
        return new UserProfile(user.getUserId(), user.getUserName(), user.getEmail(), user.getTeamId());
    }

    @Transactional
    public synchronized UserProfile updateProfile(long currentUserId, String userName, String email,
                                                  String currentPassword, String newPassword) {
        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Account non disponibile"));
        if (isBlank(currentPassword) || !PasswordHasher.verifyPassword(currentPassword, user.getPasswordHash()))
            throw new IllegalArgumentException("Credenziali non valide");
        if (isBlank(userName)) throw new IllegalArgumentException("Username obbligatorio");
        String name = userName.trim();
        if (userRepository.findByUserName(name).filter(other -> other.getUserId() != currentUserId).isPresent())
            throw new IllegalArgumentException("Username già utilizzato");
        String normalizedEmail = isBlank(email) ? null : email.trim();
        if (normalizedEmail != null && !normalizedEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Email non valida");
        if (newPassword != null && isBlank(newPassword)) throw new IllegalArgumentException("Nuova password non valida");
        String hash = newPassword == null ? user.getPasswordHash() : PasswordHasher.hashPassword(newPassword);
        user.setUserName(name);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(hash);
        userRepository.save(user);
        sessionInvalidators.forEach(s -> s.invalidateUser(currentUserId));
        return new UserProfile(user.getUserId(), user.getUserName(), user.getEmail(), user.getTeamId());
    }

    @Transactional
    public synchronized void deactivateProfile(long currentUserId, String currentPassword) {
        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Account non disponibile"));
        if (isBlank(currentPassword) || !PasswordHasher.verifyPassword(currentPassword, user.getPasswordHash()))
            throw new IllegalArgumentException("Credenziali non valide");
        if (user.getTeamId() != null) teamController.leaveTeam(currentUserId);
        for (var invitation : invitationRepository.findByInvitedUserId(currentUserId)) {
            if (invitation.getStatus() == InvitationStatus.PENDING) {
                invitation.setStatus(InvitationStatus.CANCELLED);
                invitationRepository.save(invitation);
            }
        }
        user.setActive(false);
        user.setRecoveryCodeHash(null);
        userRepository.save(user);
        sessionInvalidators.forEach(s -> s.invalidateUser(currentUserId));
    }

    @Transactional
    public synchronized RecoveryResult recoverCredentials(String userName, String recoveryCode, String newPassword) {
        if (isBlank(userName) || isBlank(recoveryCode) || isBlank(newPassword))
            throw new IllegalArgumentException("Credenziali di recupero non valide");
        User user = userRepository.findByUserName(userName.trim()).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali di recupero non valide"));
        if (user.getRecoveryCodeHash() == null || !PasswordHasher.verifyPassword(recoveryCode, user.getRecoveryCodeHash()))
            throw new IllegalArgumentException("Credenziali di recupero non valide");
        String code = recoveryCodeGenerator.generate();
        String hash = PasswordHasher.hashPassword(newPassword);
        String recoveryHash = PasswordHasher.hashPassword(code);
        user.setPasswordHash(hash);
        user.setRecoveryCodeHash(recoveryHash);
        userRepository.save(user);
        sessionInvalidators.forEach(s -> s.invalidateUser(user.getUserId()));
        return new RecoveryResult(code);
    }

    @Transactional
    public synchronized String issueRecoveryCode(long currentUserId, String currentPassword) {
        User user = userRepository.findById(currentUserId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));
        if (isBlank(currentPassword) || !PasswordHasher.verifyPassword(currentPassword, user.getPasswordHash()))
            throw new IllegalArgumentException("Credenziali non valide");
        String code = recoveryCodeGenerator.generate();
        user.setRecoveryCodeHash(PasswordHasher.hashPassword(code));
        userRepository.save(user);
        return code;
    }

    // Raccoglie tutti i ruoli trovati nelle assegnazioni dello staff.
    public Set<StaffRole> loadStaffRoles(long staffId) {
        Set<StaffRole> roles = EnumSet.noneOf(StaffRole.class);
        // Uno staff puo' avere ruoli diversi in hackathon diversi: qui li raccogliamo tutti.
        for (StaffAssignment assignment : staffAssignmentRepository.findByStaffId(staffId)) {
            roles.add(assignment.getRole());
        }
        return roles;
    }

    private LoginResult loginUser(String userName, String password) {
        Optional<User> userOpt = userRepository.findByUserName(userName);
        if (userOpt.isEmpty()) {
            return LoginResult.userNotFound();
        }

        User user = userOpt.get();
        if (!user.isActive()) return LoginResult.invalidPassword();
        if (!PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
            return LoginResult.invalidPassword();
        }
        return LoginResult.userAuthenticated(user.getUserId());
    }

    private LoginResult loginStaff(String username, String password) {
        Optional<StaffMember> staffOpt = staffMemberRepository.findByUsername(username);
        if (staffOpt.isEmpty()) {
            return LoginResult.staffNotFound();
        }

        StaffMember staff = staffOpt.get();
        if (!PasswordHasher.verifyPassword(password, staff.getPasswordHash())) {
            return LoginResult.invalidPassword();
        }
        return LoginResult.staffAuthenticated(staff.getStaffId());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
