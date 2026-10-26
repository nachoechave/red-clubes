package com.redclubes.backend.netpagos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public interface NetPagosGateway {
    record Payer(@JsonProperty("first_name") String firstName, @JsonProperty("last_name") String lastName,
                 String email, @JsonProperty("document_type") String documentType,
                 @JsonProperty("document_number") String documentNumber, String gender,
                 @JsonProperty("locked_payer") boolean lockedPayer) {}
    record Payment(String amount, String detail, String barcode, String service) {}
    record Create(Payer payer, List<Payment> payments) {}
    record Created(String uuid, String url) {}
    record Item(String amount, String barcode, String service, Boolean paid) {}
    record Status(String uuid, String status, String totalAmount, String documentNumber, List<Item> payments) {}
    Created create(Create request);
    Status status(String uuid);

    /** No provider response bodies, API keys or payer data in exceptions. */
    class Failure extends RuntimeException {
        public Failure() { super("No se pudo confirmar la respuesta de NET Pagos"); }
    }
}
