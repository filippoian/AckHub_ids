package it.unicam.hackhub.repository.inmemory;

import it.unicam.hackhub.model.SupportRequest;
import it.unicam.hackhub.repository.SupportRequestRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemorySupportRequestRepository implements SupportRequestRepository {
    private final Map<Long, SupportRequest> requests = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public Optional<SupportRequest> findById(long requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }

    @Override
    public List<SupportRequest> findAll() {
        return new ArrayList<>(requests.values());
    }

    @Override
    public List<SupportRequest> findByHackathonId(long hackathonId) {
        List<SupportRequest> result = new ArrayList<>();
        for (SupportRequest request : requests.values()) {
            if (request.getHackathonId() == hackathonId) {
                result.add(request);
            }
        }
        return result;
    }

    @Override
    public SupportRequest save(SupportRequest supportRequest) {
        if (supportRequest.getRequestId() <= 0) {
            supportRequest.setRequestId(idGenerator.incrementAndGet());
        }
        requests.put(supportRequest.getRequestId(), supportRequest);
        return supportRequest;
    }
}
