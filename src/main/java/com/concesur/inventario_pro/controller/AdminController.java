package com.concesur.inventario_pro.controller;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.concesur.inventario_pro.service.VStockService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    @Value("${admin.properties.filepath}")
    private String externalPropertiesPath;
    private final VStockService vStockService;

    @PostConstruct
    public void init() {
        log.info("Ruta del properties externo: " + externalPropertiesPath);
    }

    // Obtener todas las propiedades actuales para local
    @GetMapping("/properties")
    public Map<String, String> getProperties() {
        Map<String, String> propertiesMap = new HashMap<>();
        try (InputStream input = new FileInputStream(externalPropertiesPath)) {
            Properties prop = new Properties();
            prop.load(input);
            prop.forEach((key, value) -> propertiesMap.put(key.toString(), value.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return propertiesMap;
    }

    @PostMapping("/updateProperty")
    public String updateProperty(@RequestParam String key, @RequestParam String value) {
        try {
            // Leer las líneas del archivo externo
            List<String> lines = Files.readAllLines(Paths.get(externalPropertiesPath), StandardCharsets.ISO_8859_1);
            List<String> updatedLines = new ArrayList<>();
            boolean updated = false;

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
                    updatedLines.add(line);
                } else if (trimmedLine.startsWith(key + "=") || trimmedLine.startsWith(key + " =")) {
                    updatedLines.add(key + "=" + value);
                    updated = true;
                } else {
                    updatedLines.add(line);
                }
            }
            if (!updated) {
                updatedLines.add(key + "=" + value);
            }
            // Sobrescribir el archivo externo
            Files.write(Paths.get(externalPropertiesPath), updatedLines, StandardCharsets.UTF_8);
            vStockService.recargarVariables();
            return "Propiedad actualizada correctamente: " + key + " = " + value;
        } catch (IOException e) {
            return "Error al actualizar la propiedad: " + e.getMessage();
        }
    }
}
