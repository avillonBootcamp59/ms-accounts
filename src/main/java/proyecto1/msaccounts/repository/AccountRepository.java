package proyecto1.msaccounts.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import proyecto1.msaccounts.entity.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {
    Mono<Account> findByNumber(String number);
    Flux<Account> findByCustomerId(String numberDocument);
}

