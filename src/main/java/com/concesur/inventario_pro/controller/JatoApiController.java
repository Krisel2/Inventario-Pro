package com.concesur.inventario_pro.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import com.concesur.inventario_pro.dto.VersionDTO;
import com.concesur.inventario_pro.service.JatoApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Controller
@Slf4j
@RequestMapping("/api/jato")
@RequiredArgsConstructor
public class JatoApiController {

    private final JatoApiService jatoApiService;

    @GetMapping("/marcas")
    @ResponseBody
    public List<String> getMarcas() {
        return jatoApiService.getMarcas();
    }

    @GetMapping("/modelos/{marca}")
    @ResponseBody
    public List<String> getModelos(@PathVariable String marca) {
        return jatoApiService.getModelos(marca);
    }

    @GetMapping("/anyos/{marca}/{modelo}")
    @ResponseBody
    public List<String> getAnyos(@PathVariable String marca, @PathVariable String modelo) {
        return jatoApiService.getAnyos(marca, modelo);
    }

    @GetMapping("/fuel/{marca}/{modelo}/{anyo}")
    @ResponseBody
    public List<String> getFuel(@PathVariable String marca, @PathVariable String modelo, @PathVariable String anyo) {
        return jatoApiService.getFuel(marca, modelo, anyo);
    }

    @GetMapping("/cambio/{marca}/{modelo}/{anyo}/{fuel}")
    @ResponseBody
    public List<String> getCambio(@PathVariable String marca, @PathVariable String modelo, @PathVariable String anyo, @PathVariable String fuel) {
        return jatoApiService.getCambio(marca, modelo, anyo, fuel);
    }

    @GetMapping("/carroceria/{marca}/{modelo}/{anyo}/{fuel}/{cambio}")
    @ResponseBody
    public List<String> getCarroceria(@PathVariable String marca, @PathVariable String modelo, @PathVariable String anyo, @PathVariable String fuel, @PathVariable String cambio) {
        return jatoApiService.getCarroceria(marca, modelo, anyo, fuel, cambio);
    }

    @GetMapping("/version/{marca}/{modelo}/{anyo}/{fuel}/{cambio}/{carroceria}")
    @ResponseBody
    public List<VersionDTO> getVersion(@PathVariable String marca, @PathVariable String modelo, @PathVariable String anyo,
                                       @PathVariable String fuel, @PathVariable String cambio, @PathVariable String carroceria) {
        return jatoApiService.getVersion(marca, modelo, anyo, fuel, cambio, carroceria);
    }

    @GetMapping("/equipamiento/{vehicleId}")
    @ResponseBody
    public List<String> getEquipamiento(@PathVariable String vehicleId) {
        return jatoApiService.getEquipamiento(vehicleId);
    }

    @GetMapping("/getJato")
    public RedirectView obtenerJato(@RequestParam String idFicha, @RequestParam String Matricula) {
        try {
            String jato = idFicha;
            if(idFicha.length() < 10 ) {
                jato = jatoApiService.leerJato(idFicha);
            }
            String url = "/inventario/jato?vehicleId=" + jato + "&Matricula=" + Matricula;
            return new RedirectView(url);
        } catch (Exception e) {
            return new RedirectView("/inventario/jato?error=ficha-no-encontrada");
        }
    }

//    @GetMapping("/getJatoInfo")
//    @ResponseBody
//    public ResponseEntity<?> obtenerInfoJato(@RequestParam String idFicha) {
//        try {
//            JatoInfoDTO info = jatoApiService.obtenerInfoJato(idFicha);
//            if (info == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Información no encontrada");
//            }
//            return ResponseEntity.ok(info);
//        } catch (JsonProcessingException e) {
//            log.error("Se ha producido un error al obtener los datos", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error procesando la respuesta");
//        }
//    }


}
