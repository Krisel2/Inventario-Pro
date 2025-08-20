package com.concesur.inventario_pro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "v_stock_2")
public class VStockEntity {
    @Id
    @Column(name = "ficha_id")
    private Long fichaId;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "workflow_estado")
    private String workflowEstado;

    @Column(name = "codigo_jato_inv")
    private String codigoJato;

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "marca")
    private String marca;

    @Column(name = "version_inv")
    private String version;

    @Column(name = "matricula")
    private String matricula;

    @Column(name = "kms")
    private Integer kms;

    @Column(name = "color")
    private String color;

    @Column(name = "interior")
    private String interior;

    @Column(name = "tipo_transmision")
    private String transmision;

    @Column(name = "fecha_matriculacion")
    private String fechaMatric;

    @Column(name = "publicar_inv")
    private String publicado;

    @Column(name = "fecha_publicado_inv")
    private String fechaPublicado;

    @Column(name = "pvp")
    private String pvp;

    @Column(name = "precio_base_api")
    private String precioBase;

    @Column(name = "origen")
    private String origen;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "fecha_factura_compra")
    private String fechaFacturaCompra;

    @Column(name = "vehicle_stock_id")
    private String vehicleStockId;

}
