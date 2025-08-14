package com.concesur.inventario_pro.controller;

import com.concesur.inventario_pro.dto.VStockResponseDTO;
import com.concesur.inventario_pro.service.VStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VStockController {

    private final VStockService vStockService;

    @GetMapping("/vstock")
    public List<VStockResponseDTO> getStockDisponible() {
        return vStockService.obtenerStockFiltrado();
    }

    @PostMapping("/actualizarPublicaciones")
    public ResponseEntity<String> actualizarPublicaciones(@RequestBody VStockResponseDTO publicaciones) {
        vStockService.actualizarPublicaciones(publicaciones);
        return ResponseEntity.ok("Actualización realizada");
    }

    @GetMapping("/vstock/{id}")
    public ResponseEntity<VStockResponseDTO> obtenerVehiculoPorId(@PathVariable Long id) {
        VStockResponseDTO dto = vStockService.obtenerVehiculoPorId(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/actualizarJato")
    public ResponseEntity<String> actualizarJato(@RequestParam String idFicha, @RequestParam String codigoJato) {
        vStockService.actualizarJato(idFicha,codigoJato);
        return ResponseEntity.ok("Actualización realizada");
    }

    @GetMapping("/datos-publicado/{idFicha}")
    public ResponseEntity<Map<String, String>> obtenerDatosPublicado(@PathVariable Long idFicha) {
        Map<String, String> datos = vStockService.obtenerDatosPublicadoInv(idFicha);
        return ResponseEntity.ok(datos);
    }

//    @PostMapping("/token-autoline")
//    public ResponseEntity<String> obtenerTokenAutoline() {
//        try {
//            String token = vStockService.getTokenAL();
//            return ResponseEntity.ok(token);
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Error al obtener el token: " + e.getMessage());
//        }
//    }

}
