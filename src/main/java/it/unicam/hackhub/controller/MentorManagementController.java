package it.unicam.hackhub.controller;

import it.unicam.hackhub.model.Hackathon;
import it.unicam.hackhub.model.StaffAssignment;
import it.unicam.hackhub.model.StaffMember;
import it.unicam.hackhub.model.enums.StaffRole;
import it.unicam.hackhub.repository.HackathonRepository;
import it.unicam.hackhub.repository.StaffAssignmentRepository;
import it.unicam.hackhub.repository.StaffMemberRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MentorManagementController {
    private final HackathonRepository hackathonRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;

    public MentorManagementController(HackathonRepository hackathonRepository,
                                      StaffMemberRepository staffMemberRepository,
                                      StaffAssignmentRepository staffAssignmentRepository) {
        this.hackathonRepository = hackathonRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
    }

    // Aggiunge un mentor a un hackathon, solo se richiesto da un organizer assegnato.
    @org.springframework.transaction.annotation.Transactional
    public void addMentor(long currentStaffId, long hackathonId, long mentorStaffId) {
        Hackathon event = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        if (event.isClosed()) throw new IllegalStateException("Evento concluso");

        // Solo organizer di quell'hackathon puo' aggiungere mentor.
        boolean isOrganizer = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, StaffRole.ORGANIZER)
                .stream()
                .anyMatch(assignment -> assignment.getStaffId() == currentStaffId);
        if (!isOrganizer) {
            throw new IllegalArgumentException("Non autorizzato: solo organizer");
        }

        if (mentorStaffId <= 0) {
            throw new IllegalArgumentException("Mentor id non valido");
        }
        staffMemberRepository.findById(mentorStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff non trovato: " + mentorStaffId));

        // Evita assegnazioni mentor duplicate nello stesso hackathon.
        boolean alreadyMentor = staffAssignmentRepository.findByHackathonId(hackathonId).stream()
                .anyMatch(assignment -> assignment.getStaffId() == mentorStaffId && assignment.getRole() == StaffRole.MENTOR);
        if (alreadyMentor) {
            throw new IllegalArgumentException("Staff gia mentor su questo hackathon: " + mentorStaffId);
        }

        staffAssignmentRepository.save(new StaffAssignment(
                0L,
                mentorStaffId,
                hackathonId,
                StaffRole.MENTOR
        ));
    }

    // Elenca gli hackathon in cui lo staff corrente e' organizer.
    public List<OrganizerHackathonView> listOrganizerHackathons(long currentStaffId) {
        List<OrganizerHackathonView> result = new ArrayList<>();
        for (StaffAssignment assignment : staffAssignmentRepository.findByStaffId(currentStaffId)) {
            if (assignment.getRole() != StaffRole.ORGANIZER) {
                continue;
            }
            hackathonRepository.findById(assignment.getHackathonId()).ifPresent(hackathon ->
                    result.add(new OrganizerHackathonView(
                            hackathon.getHackathonId(),
                            safe(hackathon.getHackathonName()),
                            hackathon.getStatus() == null ? "-" : hackathon.getStatus().name(),
                            safe(hackathon.getLocation())
                    ))
            );
        }
        return result;
    }

    // Restituisce i candidati mentor non ancora assegnati a quell'hackathon.
    public List<MentorCandidateView> listMentorCandidates(long currentStaffId, long hackathonId) {
        Hackathon event = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new IllegalArgumentException("Hackathon non trovato"));
        if (event.isClosed()) throw new IllegalStateException("Evento concluso");
        boolean isOrganizer = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, StaffRole.ORGANIZER)
                .stream()
                .anyMatch(assignment -> assignment.getStaffId() == currentStaffId);
        if (!isOrganizer) {
            throw new IllegalArgumentException("Non autorizzato: solo organizer");
        }

        Set<Long> existingMentors = staffAssignmentRepository.findByHackathonIdAndRole(hackathonId, StaffRole.MENTOR)
                .stream()
                .map(StaffAssignment::getStaffId)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        List<MentorCandidateView> result = new ArrayList<>();
        for (StaffMember staffMember : staffMemberRepository.findAll()) {
            if (existingMentors.contains(staffMember.getStaffId())) {
                continue;
            }
            result.add(new MentorCandidateView(
                    staffMember.getStaffId(),
                    safe(staffMember.getStaffUsername()),
                    safe(staffMember.getStaffName())
            ));
        }
        return result;
    }

    // Aggiunge piu' mentor in una sola operazione, ignorando i duplicati in input.
    @org.springframework.transaction.annotation.Transactional
    public void addMentors(long currentStaffId, long hackathonId, List<Long> mentorStaffIds) {
        if (mentorStaffIds == null || mentorStaffIds.isEmpty()) {
            throw new IllegalArgumentException("Seleziona almeno un mentor");
        }

        Set<Long> uniqueMentorIds = new LinkedHashSet<>(mentorStaffIds);
        Set<Long> selectable = listMentorCandidates(currentStaffId, hackathonId).stream()
                .map(MentorCandidateView::staffId).collect(java.util.stream.Collectors.toSet());
        if (uniqueMentorIds.stream().anyMatch(id -> id == null || !selectable.contains(id)))
            throw new IllegalArgumentException("Mentore inesistente o già assegnato");
        for (long mentorId : uniqueMentorIds) addMentor(currentStaffId, hackathonId, mentorId);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    public record OrganizerHackathonView(long hackathonId, String name, String status, String location) {
    }

    public record MentorCandidateView(long staffId, String username, String name) {
    }
}
