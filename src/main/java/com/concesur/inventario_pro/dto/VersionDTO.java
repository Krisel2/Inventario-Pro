package com.concesur.inventario_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VersionDTO {
    private String vehicle_id;
    private String version_name;
}
