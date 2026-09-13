package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.Submission;
import it.unicam.hackhub.repository.SubmissionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissionSpringDataRepository
        extends JpaRepository<Submission, Long>, SubmissionRepository {
    @Override
    default Optional<Submission> findById(long submissionId) {
        return findById(Long.valueOf(submissionId));
    }

    Optional<Submission> findByRegistrationId(long registrationId);
}

