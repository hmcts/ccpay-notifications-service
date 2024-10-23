package uk.gov.hmcts.reform.notifications.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.notifications.model.ServiceContact;

import java.util.Optional;

@Repository
public interface ServiceContactRepository extends CrudRepository<ServiceContact, Integer> {

    Optional<ServiceContact> findByServiceName(String serviceName);
}
