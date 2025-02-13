package proyecto1.msaccounts.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import proyecto1.msaccounts.dto.CustomerDTO;

@FeignClient(name = "ms-customer", url = "http://localhost:8081/v1.0/customers")
public interface CustomerClient {
    @GetMapping("/{customerId}")
    default CustomerDTO getCustomerById(@PathVariable("customerId") String customerId) {
        try {
            return getCustomer(customerId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }
    }

    @GetMapping("/{customerId}")
    CustomerDTO getCustomer(@PathVariable("customerId") String customerId);
}
