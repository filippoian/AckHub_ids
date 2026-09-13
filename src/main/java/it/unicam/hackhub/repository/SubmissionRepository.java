package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.Submission;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository {
    Optional<Submission> findByRegistrationId(long registrationId);
    Optional<Submission> findById(long submissionId);
    List<Submission> findAll();
    Submission save(Submission submission);
}
