package com.bank.pe.msaccounts.dto;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@Data
public class CustomerDTO {
    private String id;
    private String name;
    private String type; // Personal o Empresarial
    private String numberDocument; // DNI o RUC
    private String email;
    private String profile; // VIP, PYME
}
