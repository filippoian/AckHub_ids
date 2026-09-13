package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.Submission;
import it.unicam.hackhub.repository.SubmissionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemorySubmissionRepository implements SubmissionRepository {
    private final Map<Long, Submission> byId = new HashMap<>();
    private final Map<Long, Long> registrationToSubmissionId = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<Submission> findByRegistrationId(long registrationId) {
        Long submissionId = registrationToSubmissionId.get(registrationId);
        if (submissionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(submissionId));
    }

    @Override
    public Optional<Submission> findById(long submissionId) {
        return Optional.ofNullable(byId.get(submissionId));
    }

    @Override
    public List<Submission> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public Submission save(Submission submission) {
        long previousRegistrationId = -1;
        if (submission.getSubmissionId() > 0) {
            Submission existing = byId.get(submission.getSubmissionId());
            if (existing != null) {
                previousRegistrationId = existing.getRegistrationId();
            }
        }

        if (submission.getSubmissionId() <= 0) {
            submission.setSubmissionId(idGenerator.incrementAndGet());
        }

        Long existingSubmissionForRegistration = registrationToSubmissionId.get(submission.getRegistrationId());
        if (existingSubmissionForRegistration != null
                && existingSubmissionForRegistration.longValue() != submission.getSubmissionId()) {
            throw new IllegalStateException("Submission already exists for registration (invariant violation)");
        }
        if (previousRegistrationId != -1 && previousRegistrationId != submission.getRegistrationId()) {
            registrationToSubmissionId.remove(previousRegistrationId);
        }

        byId.put(submission.getSubmissionId(), submission);
        registrationToSubmissionId.put(submission.getRegistrationId(), submission.getSubmissionId());
        return submission;
    }
}
