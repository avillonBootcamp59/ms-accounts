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
import org.springframework.web.server.ResponseStatusException;
import proyecto1.msaccounts.service.AccountService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import proyecto1.msaccounts.entity.Account;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/v1.0/accounts")
@RequiredArgsConstructor
@Tag(name = "Account API", description = "Gestión de cuentas bancarias")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    @Operation(summary = "Obtener todas las cuentas", description = "Lista todas las cuentas bancarias")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuentas obtenidas correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public Flux<Account> getAllAccounts() {
        logger.info("Obteniendo todas las cuentas bancarias");
        return accountService.listAccounts();
    }

    @Operation(summary = "Obtener una cuenta por ID", description = "Obtiene los detalles de una cuenta por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Account>> getAccountById(@PathVariable String id) {
        return accountService.getAccount(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear una cuenta bancaria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cuenta creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error en la validación de la cuenta"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping
    public Mono<ResponseEntity<Map<String, String>>> createAccount(@RequestBody Account account) {
        return accountService.createAccount(account)
                .map(savedAccount -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Cuenta creada exitosamente", "id", savedAccount.getId())))
                .onErrorResume(ResponseStatusException.class, ex -> {
                    Map<String, String> errorResponse = Map.of(
                            "error", ex.getReason(),
                            "status", String.valueOf(ex.getRawStatusCode())
                    );
                    return Mono.just(ResponseEntity.status(ex.getRawStatusCode()).body(errorResponse));
                });
    }


    @Operation(summary = "Actualizar saldo de una cuenta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Account>> updateAccount(@PathVariable String id, @RequestBody Account updatedAccount) {
        logger.info("Actualizando saldo de la cuenta con ID: {}", id);

        return accountService.updateAccount(id, updatedAccount)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar una cuenta bancaria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteAccount(@PathVariable String id) {
        logger.info("Eliminando cuenta con ID: {}", id);

        return accountService.deleteAccount(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Transferencia de fondos entre cuentas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transferencia realizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Saldo insuficiente"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @PostMapping("/transfer")
    public Mono<ResponseEntity<String>> transferFunds(@RequestParam String fromAccountId,
                                                      @RequestParam String toAccountId,
                                                      @RequestParam Double amount) {
        logger.info("Realizando transferencia de {} desde la cuenta {} a la cuenta {}",
                amount, fromAccountId, toAccountId);

        return accountService.transferBetweenAccounts(fromAccountId, toAccountId, amount)
                .then(Mono.just(ResponseEntity.ok("Transferencia realizada con éxito")))
                .onErrorResume(ResponseStatusException.class, ex -> {
                    logger.error("Error en transferencia: {}", ex.getReason());
                    return Mono.just(ResponseEntity.status(ex.getRawStatusCode()).body(ex.getReason()));
                });
    }

    @Operation(summary = "Obtener reporte de saldo promedio diario de un cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte generado con éxito"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/report/daily-balance/{customerId}")
    public Mono<ResponseEntity<Map<String, Double>>> getDailyBalanceReport(@PathVariable String customerId) {
        logger.info("Generando reporte de saldo promedio diario para el cliente: {}", customerId);

        return accountService.getDailyBalanceReport(customerId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener reporte de comisiones cobradas en un período de tiempo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte generado con éxito"),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas")
    })
    @GetMapping("/report/commissions")
    public Flux<Account> getCommissionReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        // Convertir los parámetros de String a LocalDate
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);

        logger.info("Generando reporte de comisiones desde {} hasta {}", start, end);

        // Validación de fechas
        if (start.isAfter(end)) {
            return Flux.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La fecha de inicio debe ser anterior a la fecha de fin"));
        }

        return accountService.getCommissionReport(start, end);
    }


    @Operation(summary = "Obtener una cuenta por ID cliente", description = "Obtiene las cuentas registradas por cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @GetMapping("/customer/{id}")
    public Flux<Account> getAccountsByCustomer(@PathVariable String id) {
        return accountService.getAccountsByCustomer(id)
                .switchIfEmpty(Flux.empty());
    }

}

