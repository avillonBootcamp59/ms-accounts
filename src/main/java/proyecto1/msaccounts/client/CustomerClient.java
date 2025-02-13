package proyecto1.msaccounts.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import proyecto1.msaccounts.dto.CustomerDTO;

@FeignClient(name = "ms-customer", url = "http://localhost:8081/v1.0/customers")
public interface CustomerClient {

    @GetMapping("/{customerId}")
    CustomerDTO getCustomerById(@PathVariable("customerId") String customerId);
}
