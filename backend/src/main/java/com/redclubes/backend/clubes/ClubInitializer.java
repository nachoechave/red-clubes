package com.redclubes.backend.clubes;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "redclubes.demo.enabled", havingValue = "true")
@Order(1)
public class ClubInitializer implements CommandLineRunner {

    private final ClubRepository clubRepository;

    public ClubInitializer(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    @Override
    public void run(String... args) {
        crearSiNoExiste("Club San Martin", "Av. Libertad 123");
        crearSiNoExiste("Centro de Jubilados", "Calle 45 678");
        crearSiNoExiste("Club Union", "Av. 7 890");
    }

    private void crearSiNoExiste(String nombre, String direccion) {
        if (!clubRepository.existsByNombre(nombre)) {
            clubRepository.save(new Club(nombre, direccion, EstadoClub.ACTIVO));
        }
    }
}
