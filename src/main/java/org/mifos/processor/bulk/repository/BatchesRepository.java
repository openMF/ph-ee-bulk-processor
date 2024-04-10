package org.mifos.processor.bulk.repository;

import org.mifos.processor.bulk.domain.Batches;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchesRepository extends JpaRepository<Batches, Long> {

    Boolean existsByCorrelationId(String correlationId);
}
