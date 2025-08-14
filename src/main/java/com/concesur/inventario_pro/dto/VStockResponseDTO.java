package com.concesur.inventario_pro.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class VStockResponseDTO {
    private Long fichaId;
    private Long workflowId;
    private String workflowEstado;
    private String modelo;
    private String marca;
    private String version;
    private String matricula;
    private Integer kms;
    private String color;
    private String interior;
    private String transmision;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate fechaMatric;
    private String pvp;
    private String precioBase;
    private String origen;
    private String publicado;
    private String fechaPublicado;
    private String codigoJato;
    private String ubicacion;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate fechaFacturaCompra;
    private String vehicleStockId;
    /** Se usará para ocultar el boton “/AltaCoche” en el frontend */
    private String refAnuncio;
}
