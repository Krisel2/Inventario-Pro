package com.concesur.inventario_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoAnuncioRequestDTO {
    private String estado;
    private String nombre;
    private String apellidos;
    private String telefono;
    private String email;
    private String pago_metodo;
    private String pago_cantidad;
    private String pago_transaccion;
    private String comentarios;
}
