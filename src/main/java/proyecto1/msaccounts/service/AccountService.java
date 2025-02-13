package proyecto1.msaccounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto1.msaccounts.client.CustomerClient;
import proyecto1.msaccounts.dto.CustomerDTO;
import proyecto1.msaccounts.entity.Account;
import proyecto1.msaccounts.repository.AccountRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerClient customerClient; // Feign Client para llamar a ms-customer

    public Mono<Account> validateAndCreateAccount(Account account) {
        CustomerDTO customer = customerClient.getCustomerById(account.getCustomerId());

        if (customer == null) {
            return Mono.error(new RuntimeException("Cliente no encontrado."));
        }

        if (customer.getType().equalsIgnoreCase("PERSONAL")) {
            return validatePersonalCustomer(account);
        } else if (customer.getType().equalsIgnoreCase("EMPRESARIAL")) {
            return validateBusinessCustomer(account);
        } else {
            return Mono.error(new RuntimeException("Tipo de cliente no válido."));
        }
    }

    private Mono<Account> validatePersonalCustomer(Account account) {
        return accountRepository.findByCustomerId(account.getCustomerId())
                .count()
                .flatMap(count -> {
                    if (account.getType().equalsIgnoreCase("AHORRO") && count > 0
                    || account.getType().equalsIgnoreCase("PLAZO_FIJO") && count > 0
                    || account.getType().equalsIgnoreCase("CORRIENTE") && count > 0) {
                        return Mono.error(new RuntimeException("Un cliente personal solo puede tener una cuenta de ahorro,una cuenta corriente o cuentas a plazo fijo."));
                    }
                    return accountRepository.save(account);
                });
    }

    private Mono<Account> validateBusinessCustomer(Account account) {
        if (account.getType().equalsIgnoreCase("AHORRO") || account.getType().equalsIgnoreCase("PLAZO_FIJO")) {
            return Mono.error(new RuntimeException("Un cliente empresarial no puede tener cuentas de ahorro o de plazo fijo."));
        }
        return accountRepository.save(account);
    }
}