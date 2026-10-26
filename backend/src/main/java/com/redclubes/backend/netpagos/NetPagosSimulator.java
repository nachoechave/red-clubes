package com.redclubes.backend.netpagos;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Sandbox-only, finite in-memory provider. Loss on restart never implies cancellation or payment. */
public class NetPagosSimulator implements NetPagosGateway {
    private final Map<String, Status> orders = new ConcurrentHashMap<>();
    @Override public synchronized Created create(Create payload) {
        if (orders.size() >= 1000) throw new Failure();
        String id = UUID.randomUUID().toString();
        String total = payload.payments().stream().map(p -> new BigDecimal(p.amount())).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString();
        orders.put(id, new Status(id, "Instanciado", total, payload.payer().documentNumber(), payload.payments().stream()
                .map(p -> new Item(p.amount(), p.barcode(), p.service(), null)).toList()));
        return new Created(id, "https://checkout.example.invalid/" + id);
    }
    @Override public Status status(String uuid) {
        var order = orders.get(uuid); if (order == null) throw new Failure(); return order;
    }
    public void advance(String uuid, String state, List<Boolean> paid) {
        Status old = status(uuid);
        if (!List.of("Instanciado", "Proceso", "Finalizado", "Cancelado", "Parcial").contains(state)
                || paid == null || paid.size() != old.payments().size() || paid.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Simulacion invalida");
        var items = new ArrayList<Item>();
        for (int i = 0; i < paid.size(); i++) {
            Item p = old.payments().get(i); items.add(new Item(p.amount(), p.barcode(), p.service(), paid.get(i)));
        }
        orders.put(uuid, new Status(uuid, state, old.totalAmount(), old.documentNumber(), List.copyOf(items)));
    }
}
