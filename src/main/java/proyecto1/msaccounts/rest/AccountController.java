package proyecto1.msaccounts.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto1.msaccounts.service.AccountService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import proyecto1.msaccounts.entity.Account;
import proyecto1.msaccounts.repository.AccountRepository;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1.0/accounts")
@RequiredArgsConstructor
@Tag(name = "Account API", description = "Gestión de cuentas bancarias")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final AccountRepository repository;
    private final AccountService accountService;

    @Operation(summary = "Obtener todas las cuentas", description = "Lista todas las cuentas bancarias")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuentas obtenidas correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public Flux<Account> getAllAccounts() {
        logger.info("Obteniendo todas las cuentas bancarias");
        return repository.findAll();
    }

    @Operation(summary = "Obtener una cuenta por número", description = "Busca una cuenta bancaria por su número único")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @GetMapping("/{number}")
    public Mono<ResponseEntity<Account>> getAccountByNumber(@PathVariable String number) {
        return repository.findByNumber(number)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear una cuenta bancaria", description = "Registra una nueva cuenta en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cuenta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en la validación de la cuenta")
    })
    @PostMapping
    public Mono<ResponseEntity<Account>> createAccount(@Valid @RequestBody Account account) {
        logger.info("Intentando crear cuenta bancaria para cliente {}", account.getCustomerId());

        return accountService.validateAndCreateAccount(account)
                .map(savedAccount -> ResponseEntity.status(HttpStatus.CREATED).body(savedAccount))
                .onErrorResume(error -> {
                    logger.error("Error al crear cuenta bancaria: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(null));
                });
    }

    @Operation(summary = "Actualizar saldo de una cuenta", description = "Modifica el saldo de una cuenta bancaria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Account>> updateBalance(@PathVariable String id, @RequestBody Double newBalance) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setBalance(newBalance);
                    return repository.save(existing);
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar una cuenta bancaria", description = "Elimina una cuenta bancaria del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteAccount(@PathVariable String id) {
        logger.info("Eliminando cuenta con ID: {}", id);
        return repository.findById(id)
                .flatMap(existingAccount -> repository.delete(existingAccount)
                        .then(Mono.just(ResponseEntity.ok().<Void>build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
