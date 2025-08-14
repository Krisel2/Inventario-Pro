package com.concesur.inventario_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatosCoche {
    private String referencia;
    private String locationId;
    private String matriculacionDay;
    private String matriculacionMonth;
    private String matriculacionYear;
    private String matricula;
    private String numBastidor;
    private String kilometros;
    private String precio;
    private String adSubtypeId;
    private String descripcion;
    private String colorId;

}
