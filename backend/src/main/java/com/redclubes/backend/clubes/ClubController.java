package com.redclubes.backend.clubes;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clubes")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    public List<ClubResponse> listarClubes(@RequestHeader("Authorization") String authorizationHeader) {
        return clubService.listarClubesDisponibles(authorizationHeader);
    }

    @PostMapping
    public ClubResponse crearClub(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CrearClubRequest request
    ) {
        return clubService.crearClub(authorizationHeader, request);
    }

    @PutMapping("/{clubId}")
    public ClubResponse actualizarClub(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long clubId,
            @Valid @RequestBody ActualizarClubRequest request
    ) {
        return clubService.actualizarClub(authorizationHeader, clubId, request);
    }
}
