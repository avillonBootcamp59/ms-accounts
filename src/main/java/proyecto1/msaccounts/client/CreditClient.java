package proyecto1.msaccounts.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import proyecto1.msaccounts.dto.CreditDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CreditClient {

    private final WebClient webClient;

    public CreditClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8083").build();
    }

    public Flux<CreditDTO> getCreditProductsByCustomer(String customerId) {
        return webClient.get()
                .uri("/v1.0/credits/customer/{id}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron créditos para este cliente")))
                .onStatus(HttpStatus::is5xxServerError, response ->
                        Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error en el servicio ms-credits")))
                .bodyToFlux(CreditDTO.class);
    }
    public Mono<Boolean> hasOverdueDebt(String customerId) {
        return webClient.get()
                .uri("/v1.0/credits/hasOverdueDebt/{customerId}", customerId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorReturn(false);
    }

}

