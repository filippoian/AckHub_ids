package it.unicam.hackhub.cli;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.StaffMember;
import it.unicam.hackhub.model.enums.HackathonStatus;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.StaffMemberRepository;
import it.unicam.hackhub.security.PasswordHasher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DataSeeder {
    private final StaffMemberRepository staffMemberRepository;
    private final HackathonRepository hackathonRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;

    public DataSeeder(StaffMemberRepository staffMemberRepository, HackathonRepository hackathonRepository, StaffAssignmentRepository staffAssignmentRepository) {
        this.staffMemberRepository = staffMemberRepository;
        this.hackathonRepository = hackathonRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
    }

    // Inserisce dati demo minimi per usare la CLI in locale.
    public void seed() {
        StaffMember organizer = staffMemberRepository.save(new StaffMember(
                0,
                "Olivia Organizer",
                "organizer1",
                PasswordHasher.hashPassword("organizer1")
        ));
        StaffMember judge = staffMemberRepository.save(new StaffMember(
                0,
                "Jack Judge",
                "judge1",
                PasswordHasher.hashPassword("judge1")
        ));
        StaffMember mentorOne = staffMemberRepository.save(new StaffMember(
                0,
                "Marta Mentor",
                "mentor1",
                PasswordHasher.hashPassword("mentor1")
        ));
        StaffMember mentorTwo = staffMemberRepository.save(new StaffMember(
                0,
                "Marco Mentor",
                "mentor2",
                PasswordHasher.hashPassword("mentor2")
        ));

        Hackathon registrationHackathon = hackathonRepository.save(Hackathon.builder()
                .hackathonId(0)
                .hackathonName("HackHub Starter")
                .regulation("Starter regulation")
                .registrationDeadline(LocalDateTime.now().plusDays(7))
                .startDate(LocalDateTime.now().plusDays(10))
                .endDate(LocalDateTime.now().plusDays(12))
                .submissionDeadline(LocalDateTime.now().plusDays(11))
                .location("Online")
                .prizeAmount(BigDecimal.valueOf(1000))
                .maxTeamSize(5)
                .status(HackathonStatus.REGISTRATION)
                .winnerTeamId(null)
                .build());

        Hackathon runningHackathon = hackathonRepository.save(Hackathon.builder()
                .hackathonId(0)
                .hackathonName("HackHub Pro")
                .regulation("Pro regulation")
                .registrationDeadline(LocalDateTime.now().minusDays(5))
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(2))
                .submissionDeadline(LocalDateTime.now().plusDays(1))
                .location("Campus Lab")
                .prizeAmount(BigDecimal.valueOf(2500))
                .maxTeamSize(6)
                .status(HackathonStatus.RUNNING)
                .winnerTeamId(null)
                .build());

        staffAssignmentRepository.save(new StaffAssignment(0, organizer.getStaffId(), registrationHackathon.getHackathonId(), StaffRole.ORGANIZER));
        staffAssignmentRepository.save(new StaffAssignment(0, judge.getStaffId(), registrationHackathon.getHackathonId(), StaffRole.JUDGE));
        staffAssignmentRepository.save(new StaffAssignment(0, mentorOne.getStaffId(), registrationHackathon.getHackathonId(), StaffRole.MENTOR));

        staffAssignmentRepository.save(new StaffAssignment(0, organizer.getStaffId(), runningHackathon.getHackathonId(), StaffRole.ORGANIZER));
        staffAssignmentRepository.save(new StaffAssignment(0, judge.getStaffId(), runningHackathon.getHackathonId(), StaffRole.JUDGE));
        staffAssignmentRepository.save(new StaffAssignment(0, mentorTwo.getStaffId(), runningHackathon.getHackathonId(), StaffRole.MENTOR));
    }
}
