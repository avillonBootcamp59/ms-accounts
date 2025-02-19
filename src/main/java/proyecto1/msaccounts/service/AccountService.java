package proyecto1.msaccounts.service;
 
import proyecto1.msaccounts.entity.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.util.Map;

public interface AccountService {
  public Flux<Account> listAccounts();
  public Mono<Account> getAccount(String id);
  public Mono<Void> deleteAccount(String id);
  public Mono<Account> createAccount(Account Account);
  public Mono<Account> updateAccount(String id, Account updatedAccount);
  public Mono<Account> updateBalanceAccount(String id, Double mount);
  public Flux<Account> getCommissionReport(LocalDate startDate, LocalDate endDate);
  public Mono<Map<String, Double>> getDailyBalanceReport(String customerId);
  public Mono<Void> transferBetweenAccounts(String fromAccountId, String toAccountId, Double amount);
  public Flux<Account> getAccountsByCustomer(String id);
}
