package it.unicam.hackhub.bootstrap;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.StaffMember;
import it.unicam.hackhub.model.enums.HackathonStatus;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.StaffMemberRepository;
import it.unicam.hackhub.security.PasswordHasher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final String ORGANIZER_USERNAME = "organizer1";
    private static final String JUDGE_USERNAME = "judge1";
    private static final String MENTOR_USERNAME = "mentor1";
    private static final String ORGANIZER_TWO_USERNAME = "organizer2";
    private static final String JUDGE_TWO_USERNAME = "judge2";
    private static final String MENTOR_TWO_USERNAME = "mentor2";

    private final StaffMemberRepository staffMemberRepository;
    private final HackathonRepository hackathonRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;

    public DataSeeder(StaffMemberRepository staffMemberRepository,
                      HackathonRepository hackathonRepository,
                      StaffAssignmentRepository staffAssignmentRepository) {
        this.staffMemberRepository = staffMemberRepository;
        this.hackathonRepository = hackathonRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
    }

    @Override
    // Entry point Spring del seed dati.
    public void run(String... args) {
        seed();
    }

    // Crea o riusa dati demo base per staff, hackathon e ruoli.
    public void seed() {
        // I metodi ensure* evitano duplicati se il seed gira piu' volte.
        StaffMember organizerOne = ensureStaff("Organizer One", ORGANIZER_USERNAME, "organizer1pass");
        StaffMember judgeOne = ensureStaff("Judge One", JUDGE_USERNAME, "judge1pass");
        StaffMember mentorOne = ensureStaff("Mentor One", MENTOR_USERNAME, "mentor1pass");
        StaffMember organizerTwo = ensureStaff("Organizer Two", ORGANIZER_TWO_USERNAME, "organizer2pass");
        StaffMember judgeTwo = ensureStaff("Judge Two", JUDGE_TWO_USERNAME, "judge2pass");
        StaffMember mentorTwo = ensureStaff("Mentor Two", MENTOR_TWO_USERNAME, "mentor2pass");

        Hackathon hackathonOne = ensureHackathon(
                "HackHub Demo 1",
                "Regolamento demo 1",
                LocalDateTime.of(2026, 3, 15, 23, 59),
                LocalDateTime.of(2026, 3, 20, 9, 0),
                LocalDateTime.of(2026, 3, 22, 18, 0),
                LocalDateTime.of(2026, 3, 21, 20, 0),
                "Ancona",
                new BigDecimal("2500.00"),
                5,
                HackathonStatus.REGISTRATION
        );

        Hackathon hackathonTwo = ensureHackathon(
                "HackHub Demo 2",
                "Regolamento demo 2",
                LocalDateTime.of(2026, 4, 10, 23, 59),
                LocalDateTime.of(2026, 4, 15, 9, 0),
                LocalDateTime.of(2026, 4, 17, 18, 0),
                LocalDateTime.of(2026, 4, 16, 20, 0),
                "Camerino",
                new BigDecimal("5000.00"),
                4,
                HackathonStatus.RUNNING
        );

        assignCoreRoles(organizerOne, judgeOne, mentorOne, hackathonOne);
        assignCoreRoles(organizerTwo, judgeTwo, mentorTwo, hackathonTwo);
    }

    // Restituisce lo staff esistente o lo crea con la password hashata.
    private StaffMember ensureStaff(String name, String username, String plainPassword) {
        return staffMemberRepository.findByUsername(username)
                .orElseGet(() -> staffMemberRepository.save(
                        new StaffMember(
                                0L,
                                name,
                                username,
                                PasswordHasher.hashPassword(plainPassword)
                        )
                ));
    }

    // Restituisce l'hackathon esistente con quel nome o lo crea.
    private Hackathon ensureHackathon(String name,
                                      String regulation,
                                      LocalDateTime registrationDeadline,
                                      LocalDateTime startDate,
                                      LocalDateTime endDate,
                                      LocalDateTime submissionDeadline,
                                      String location,
                                      BigDecimal prizeAmount,
                                      int maxTeamSize,
                                      HackathonStatus status) {
        return hackathonRepository.findByName(name)
                .orElseGet(() -> hackathonRepository.save(
                        Hackathon.builder()
                                .hackathonId(0L)
                                .hackathonName(name)
                                .regulation(regulation)
                                .registrationDeadline(registrationDeadline)
                                .startDate(startDate)
                                .endDate(endDate)
                                .submissionDeadline(submissionDeadline)
                                .location(location)
                                .prizeAmount(prizeAmount)
                                .maxTeamSize(maxTeamSize)
                                .status(status)
                                .winnerTeamId(null)
                                .build()
                ));
    }

    // Assegna i ruoli core minimi all'hackathon demo.
    private void assignCoreRoles(StaffMember organizer, StaffMember judge, StaffMember mentor, Hackathon hackathon) {
        ensureAssignment(organizer.getStaffId(), hackathon.getHackathonId(), StaffRole.ORGANIZER);
        ensureAssignment(judge.getStaffId(), hackathon.getHackathonId(), StaffRole.JUDGE);
        ensureAssignment(mentor.getStaffId(), hackathon.getHackathonId(), StaffRole.MENTOR);
    }

    // Salva l'assegnazione solo se non esiste gia'.
    private void ensureAssignment(long staffId, long hackathonId, StaffRole role) {
        boolean exists = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, role).stream()
                .anyMatch(assignment -> assignment.getStaffId() == staffId);
        if (!exists) {
            staffAssignmentRepository.save(new StaffAssignment(0L, staffId, hackathonId, role));
        }
    }
}
