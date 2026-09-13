package it.unicam.hackhub.repository;

import it.unicam.hackhub.model.Evaluation;

import java.util.Optional;

public interface EvaluationRepository {
    Optional<Evaluation> findBySubmissionId(long submissionId);
    Optional<Evaluation> findById(long evaluationId);
    Evaluation save(Evaluation evaluation);
}
