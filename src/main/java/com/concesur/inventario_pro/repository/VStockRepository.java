package com.concesur.inventario_pro.repository;

import com.concesur.inventario_pro.entity.VStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface VStockRepository extends JpaRepository<VStockEntity, Long> {
    @Query("SELECT v FROM VStockEntity v WHERE v.workflowId IN (6, 39) AND v.workflowEstado like ('Stock%')")
    List<VStockEntity> findStockDisponible();
}
