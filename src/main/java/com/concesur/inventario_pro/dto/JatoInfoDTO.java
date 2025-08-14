package com.concesur.inventario_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class JatoInfoDTO {
    private String marca;
    private String modelo;
}
