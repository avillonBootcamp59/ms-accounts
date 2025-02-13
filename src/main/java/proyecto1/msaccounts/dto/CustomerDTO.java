package proyecto1.msaccounts.dto;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
public class CustomerDTO {
    private String id;
    private String name;
    private String type; // Personal o Empresarial
    private String numberDocument; // DNI o RUC
    private String email;
}
