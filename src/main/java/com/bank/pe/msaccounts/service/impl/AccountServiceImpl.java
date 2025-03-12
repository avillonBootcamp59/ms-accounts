package com.bank.pe.msaccounts.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.bank.pe.msaccounts.client.CreditClient;
import com.bank.pe.msaccounts.client.CustomerClient;
import com.bank.pe.msaccounts.dto.CustomerDTO;
import com.bank.pe.msaccounts.entity.Account;
import com.bank.pe.msaccounts.repository.AccountRepository;
import com.bank.pe.msaccounts.service.AccountService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    private final AccountRepository accountRepository;
    private final CustomerClient customerClient;
    private final CreditClient creditClient;

    @Override
    public Flux<Account> listAccounts() {
        return accountRepository.findAll();
    }

    @Override
    public Mono<Account> getAccount(String id) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cuenta bancaria no encontrada")));
    }

    @Override
    public Mono<Void> deleteAccount(String id) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cuenta bancaria no encontrada")))
                .flatMap(accountRepository::delete);
    }

    @Override
    public Mono<Account> createAccount(Account account) {
        return customerClient.getCustomerById(account.getCustomerId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado")))
                .flatMap(customer -> creditClient.hasOverdueDebt(account.getCustomerId())
                        .flatMap(hasDebt -> {
                            if (hasDebt) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "Cliente con deuda de crédito vencido"));
                            }
                            return accountRepository.findByCustomerId(account.getCustomerId()).collectList()
                                    .flatMap(existingAccounts -> applyAccountRules(account, customer, existingAccounts))
                                    .flatMap(accountRepository::save);
                        }));

    }

    @Override
    public Mono<Account> updateBalanceAccount(String id, Double mount) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cuenta bancaria no encontrada")))
                .flatMap(existingAccount -> {
                    existingAccount.setBalance(mount);
                    return accountRepository.save(existingAccount);
                });
    }

    @Override
    public Mono<Account> updateAccount(String id, Account updatedAccount) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cuenta bancaria no encontrada")))
                .flatMap(existingAccount -> {
                    existingAccount.setHasMaintenanceFee(updatedAccount.isHasMaintenanceFee());
                    existingAccount.setCommissionFee(updatedAccount.getCommissionFee());
                    existingAccount.setMinimumOpeningBalance(updatedAccount.getMinimumOpeningBalance());
                    existingAccount.setFreeTransactions(updatedAccount.getFreeTransactions());
                    existingAccount.setAuthorizedSigners(updatedAccount.getAuthorizedSigners());
                    return accountRepository.save(existingAccount);
                });
    }

    private Mono<Account> applyAccountRules(Account account, CustomerDTO customer, List<Account> existingAccounts) {
        boolean isPersonal = "PERSONAL".equalsIgnoreCase(customer.getType());
        boolean isBusiness = "EMPRESARIAL".equalsIgnoreCase(customer.getType());


        // Reglas para clientes personales
        if (isPersonal) {
            boolean hasSavings = existingAccounts.stream().anyMatch(acc -> "AHORRO".equalsIgnoreCase(acc.getType()));
            boolean hasCurrent = existingAccounts.stream().anyMatch(acc -> "CORRIENTE".equalsIgnoreCase(acc.getType()));
            boolean hasFixed = existingAccounts.stream().anyMatch(acc -> "PLAZO_FIJO".equalsIgnoreCase(acc.getType()));


            if (("AHORRO".equalsIgnoreCase(account.getType()) && hasSavings) ||
                    ("CORRIENTE".equalsIgnoreCase(account.getType()) && hasCurrent) ||
                    ("PLAZO_FIJO".equalsIgnoreCase(account.getType()) && hasFixed)) {
                return Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El cliente ya tiene una cuenta de este tipo"));
            }


            if ("VIP".equalsIgnoreCase(customer.getProfile())) {
                return hasCreditCard(customer.getId())
                        .flatMap(hasCard -> {
                            if (!hasCard) {
                                return Mono.error(new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Cliente VIP requiere tarjeta de crédito activa"));
                            }
                            return Mono.just(account);
                        });
            }
        }


        // Reglas para clientes empresariales
        if (isBusiness) {
            if ("AHORRO".equalsIgnoreCase(account.getType()) || "PLAZO_FIJO".equalsIgnoreCase(account.getType())) {
                return Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Clientes empresariales no pueden tener cuentas de ahorro ni plazo fijo"));
            }


            if ("PYME".equalsIgnoreCase(customer.getProfile())) {
                return hasCreditCard(customer.getId())
                        .flatMap(hasCard -> {
                            if (!hasCard) {
                                return Mono.error(new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Cliente PYME requiere tarjeta de crédito activa"));
                            }
                            return Mono.just(account);
                        });
            }
        }

        return Mono.just(account);
    }

    private Mono<Boolean> hasCreditCard(String customerId) {
        return creditClient.getCreditProductsByCustomer(customerId)
                .any(credit -> "TARJETA_CREDITO".equalsIgnoreCase(credit.getCreditType()));
    }

    public Mono<Void> transferBetweenAccounts(String fromAccountId, String toAccountId, Double amount) {
        return accountRepository.findById(fromAccountId)
                .flatMap(fromAccount -> accountRepository.findById(toAccountId)
                        .flatMap(toAccount -> {
                            if (fromAccount.getBalance() < amount) {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente"));
                            }

                            List<Account> updatedAccounts = Stream.of(fromAccount, toAccount)
                                    .map(account -> {
                                        if (account.getId().equals(fromAccountId)) {
                                            account.setBalance(account.getBalance() - amount);
                                        } else {
                                            account.setBalance(account.getBalance() + amount);
                                        }
                                        return account;
                                    })
                                    .collect(Collectors.toList());

                            return accountRepository.saveAll(updatedAccounts).then();
                        }));
    }

    @Override
    public Flux<Account> getAccountsByCustomer(String id) {
        return accountRepository.findByCustomerId(id)
                .switchIfEmpty(Flux.empty());
    }

    @Override
    public Mono<Map<String, Double>> getDailyBalanceReport(String customerId) {
        return accountRepository.findByCustomerId(customerId)
                .collectList()
                .map(accounts -> accounts.stream()
                        .collect(Collectors.toMap(
                                Account::getNumber,
                                acc -> acc.getBalance() / LocalDate.now().getDayOfMonth()
                        )));
    }

    @Override
    public Flux<Account> getCommissionReport(LocalDate startDate, LocalDate endDate) {
        return accountRepository.findAll()
                .filter(acc -> {
                    LocalDateTime lastTransactionDateTime = acc.getLastTransactionDate();
                    if (lastTransactionDateTime == null) {
                        return false;
                    }
                    LocalDate lastTransaction = lastTransactionDateTime.toLocalDate();

                    return (lastTransaction.isEqual(startDate) || lastTransaction.isAfter(startDate))
                            && (lastTransaction.isEqual(endDate) || lastTransaction.isBefore(endDate));
                })
                .filter(Account::isHasMaintenanceFee)
                .switchIfEmpty(Flux.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontraron cuentas con comisiones cobradas en el rango de fechas indicado")));
    }


}

