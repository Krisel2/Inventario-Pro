package com.concesur.inventario_pro.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class Utils {

    private final RestTemplate restTemplate;

    public Optional<String> validarCamposObligatorios(Map<String, String> campos) {
        List<String> faltantes = new ArrayList<>();

        for (Map.Entry<String, String> entry : campos.entrySet()) {
            String valor = entry.getValue();
            if (valor == null || valor.isBlank()) {
                faltantes.add(entry.getKey());
            }
        }

        if (faltantes.isEmpty()) {
            return Optional.empty();
        }
        String mensaje = "El anuncio no se ha creado porque faltan estos campos obligatorios: "
                + String.join(", ", faltantes)
                + ". Por favor, rellénelos e inténtelo de nuevo.";

        log.error(mensaje);
        return Optional.of(mensaje);
    }

    public String extraerReferenciaDesdeLocation(String locationUrl) {
        if (locationUrl == null) return null;

        // Ejemplo: Extrae "2573102" de "/coche/fotos/2573102?creado"
        Pattern pattern = Pattern.compile("/coche/fotos/(\\d+)");
        Matcher matcher = pattern.matcher(locationUrl);

        return matcher.find() ? matcher.group(1) : null;
    }

    public String extraerReferenciaDesdeBody(String matricula) {

        Pattern pattern = Pattern.compile("/coche/editaravanzado/(\\d+)");
        Matcher matcher = pattern.matcher(matricula);

        return matcher.find() ? matcher.group(1) : null;
    }

    public String comprobarNulo(String value) {
        return value != null ? value : "";
    }

    public boolean esPrecioValido(String valor) {
        try {
            double precio = Double.parseDouble(valor.replace(",", "."));
            return precio >= 10000;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public MultiValueMap<String, String> leerTabItems(String urlGet, HttpEntity<?> requestEntity) throws RestClientException, JsonProcessingException {
        ResponseEntity<String> response = restTemplate.exchange(urlGet, HttpMethod.GET, requestEntity, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode tabItems = (ArrayNode) objectMapper.readTree(response.getBody());
        MultiValueMap<String, String> datosSalida = new LinkedMultiValueMap<>();

        for (JsonNode item : tabItems) {

            String campo = item.path("name").asText();   // «Tiendas», «Kilometros», …
            String tipo  = item.path("typeItem").asText();
            String nuevoValor = "";

            switch (tipo) {

                case "LIST" -> {
                    if ("Tiendas".equalsIgnoreCase(campo != null ? campo.trim() : "")
                            || "Color Inv".equalsIgnoreCase(campo != null ? campo.trim() : "")) {

                        nuevoValor = extraerValorListaTiendas(item);
                    } else {
                        nuevoValor = obtenerCodigoSeleccionado(item);
                    }
                }

                case "INPUT" -> nuevoValor =
                        item.path("tabItemConfigInput")
                                .path("cardTabItemInstance")
                                .path("value").asText();

                case "VARIABLE" -> nuevoValor =
                        item.path("tabItemConfigVariable")
                                .path("variable")
                                .path("value").asText();
            }

            datosSalida.set(campo, nuevoValor);
        }
        return datosSalida;
    }

    private String obtenerCodigoSeleccionado(JsonNode item) {
        String codigo = item.path("tabItemConfigList")
                .path("cardTabItemInstance")
                .path("value").asText(null);

        return (codigo == null || codigo.isBlank())
                ? item.path("value").asText("")
                : codigo;
    }

    /** Solo para «Tiendas»: traduce el código al texto legible. */
    private String extraerValorListaTiendas(JsonNode item) {

        JsonNode listConfig = item.path("tabItemConfigList");
        String codigoSel = obtenerCodigoSeleccionado(item);

        if (codigoSel.isEmpty()) return "";

        for (JsonNode li : listConfig.path("listItems")) {
            if (codigoSel.equals(li.path("code").asText())) {
                return li.path("value").asText();
            }
        }
        return codigoSel;
    }

}
