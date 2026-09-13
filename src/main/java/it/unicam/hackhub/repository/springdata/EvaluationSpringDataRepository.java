package it.unicam.hackhub.repository.springdata;

import it.unicam.hackhub.model.Evaluation;
import it.unicam.hackhub.repository.EvaluationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvaluationSpringDataRepository
        extends JpaRepository<Evaluation, Long>, EvaluationRepository {
    @Override
    default Optional<Evaluation> findById(long evaluationId) {
        return findById(Long.valueOf(evaluationId));
    }

    Optional<Evaluation> findBySubmissionId(long submissionId);
}

