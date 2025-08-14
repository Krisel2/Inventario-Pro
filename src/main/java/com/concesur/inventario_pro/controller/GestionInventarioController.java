package com.concesur.inventario_pro.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import com.concesur.inventario_pro.dto.CambioEstadoAnuncioRequestDTO;
import com.concesur.inventario_pro.service.GestionInventarioPro;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;




@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gestion")
public class GestionInventarioController {

    private final GestionInventarioPro gestionInventarioPro;

    @PostMapping("/testLogin")
    @ResponseBody
    public ResponseEntity<String> testLoginInventarioPro() {
        return ResponseEntity.ok(gestionInventarioPro.autenticarInventarioPro());
    }

    @PostMapping("/AltaCoche")
    @ResponseBody
    public ResponseEntity<String> darAltaCoche(@RequestParam String codJato, @RequestParam String idFicha) {
        return ResponseEntity.ok(gestionInventarioPro.crearFormularioAvanzado(codJato,idFicha));
    }


    @PostMapping("/buscarVehiculo")
    public ResponseEntity<Map<String, Object>> buscarVehiculo(
            @RequestParam String idFicha,
            @RequestBody(required = false) CambioEstadoAnuncioRequestDTO datosAnuncio) {

        try {
            Map<String, Object> resultado = gestionInventarioPro.buscarVehiculo(idFicha, datosAnuncio);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al buscar vehículo", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", 0);
            error.put("response", Map.of("mensaje", "Error: " + e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

//    @PostMapping("/subirFotos")
//    @ResponseBody
//    public ResponseEntity<String> subirFotos(@RequestParam String idAnuncio, String idFicha) {
//            return ResponseEntity.ok(gestionInventarioPro.adjuntarFoto(idAnuncio,idFicha));
//    }

}
