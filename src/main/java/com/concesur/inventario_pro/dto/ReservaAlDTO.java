package com.concesur.inventario_pro.dto;

import lombok.Data;

@Data
public class ReservaAlDTO {

    private Reservation reservation;
    private Metadata metadata;

    @Data
    public static class Reservation {
        private String reservationDate;
        private String reservedUntilDate;
        private ReservedBy reservedBy;
    }

    @Data
    public static class ReservedBy {
        private String salesPersonId;
        private String name;
    }

    @Data
    public static class Metadata {
        private AuditData auditData;
    }

    @Data
    public static class AuditData {
        private String userId;
        private String userName;
    }
}
