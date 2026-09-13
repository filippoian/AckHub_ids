package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.Evaluation;
import it.unicam.hackhub.repository.EvaluationRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryEvaluationRepository implements EvaluationRepository {
    private final Map<Long, Evaluation> byId = new HashMap<>();
    private final Map<Long, Long> submissionToEvaluationId = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<Evaluation> findBySubmissionId(long submissionId) {
        Long evaluationId = submissionToEvaluationId.get(submissionId);
        if (evaluationId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(evaluationId));
    }

    @Override
    public Optional<Evaluation> findById(long evaluationId) {
        return Optional.ofNullable(byId.get(evaluationId));
    }

    @Override
    public Evaluation save(Evaluation evaluation) {
        long previousSubmissionId = -1;
        if (evaluation.getEvaluationId() > 0) {
            Evaluation existing = byId.get(evaluation.getEvaluationId());
            if (existing != null) {
                previousSubmissionId = existing.getSubmissionId();
            }
        }

        if (evaluation.getEvaluationId() <= 0) {
            evaluation.setEvaluationId(idGenerator.incrementAndGet());
        }

        Long existingEvaluationForSubmission = submissionToEvaluationId.get(evaluation.getSubmissionId());
        if (existingEvaluationForSubmission != null
                && existingEvaluationForSubmission.longValue() != evaluation.getEvaluationId()) {
            throw new IllegalStateException("Evaluation already exists for submission (invariant violation)");
        }
        if (previousSubmissionId != -1 && previousSubmissionId != evaluation.getSubmissionId()) {
            submissionToEvaluationId.remove(previousSubmissionId);
        }

        byId.put(evaluation.getEvaluationId(), evaluation);
        submissionToEvaluationId.put(evaluation.getSubmissionId(), evaluation.getEvaluationId());
        return evaluation;
    }
}
