package com.concesur.inventario_pro.service;

import com.concesur.inventario_pro.dto.VersionDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JatoApiService {
    private final RestTemplate restTemplate;
    private final VStockService vStockService;
    private static final String URL_BASE_FICHA = "https://go.grupoconcesur.es/concenet-rest/api/cardInstanceWorkflow/detail/";
    private static final String TAB_JATO = "23";
    //private static final String TAB_DAT_VEHI = "1";
    @Value("${app.inventarioPRO.apikey}")
    private String apiKey;
    @Value("${app.inventarioPRO.apiUrl}")
    private String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpEntity<Void> buildEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Apikey", apiKey);
        return new HttpEntity<>(headers);
    }

    private List<String> extractFieldList(String json, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.get("data");
        List<String> result = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                result.add(item.get(fieldName).asText());
            }
        }
        return result;
    }

    private List<String> safeExtract(String json, String fieldName, String contextoError) {
        try {
            return extractFieldList(json, fieldName);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("argument \"content\" is null")) {
                return new ArrayList<>();
            }
            throw new RuntimeException("Error al obtener " + contextoError, e);
        }
    }

    public List<String> getMarcas() {
        String url = baseUrl + "/discover/makes";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
        try {
            return extractFieldList(response.getBody(), "make");
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener marcas", e);
        }
    }

    public List<String> getModelos(String marca) {
        String url = baseUrl + "/discover/makes/" + marca + "/models";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
        return safeExtract(response.getBody(), "model", "modelos");
    }

    public List<String> getAnyos(String marca, String modelo) {
        String url = baseUrl + "/discover/makes/" + marca + "/models/" + modelo + "/years";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
        return safeExtract(response.getBody(), "year", "anyos");
    }

    public List<String> getFuel(String marca, String modelo, String anyo) {
        String url = baseUrl + "/discover/makes/" + marca + "/models/" + modelo + "/years/" + anyo + "/engines";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
        return safeExtract(response.getBody(), "fuel","combustible");
    }

    public List<String> getCambio(String marca, String modelo, String anyo, String fuel) {
        String url = baseUrl + "/discover/makes/" + marca + "/models/" + modelo + "/years/" + anyo + "/engines/" + fuel + "/gear-boxes";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
        return safeExtract(response.getBody(), "gear_box_value","Cajas de cambios");
    }

    public List<String> getCarroceria(String marca, String modelo, String anyo, String fuel, String cambio) {
        String url = baseUrl + "/discover/makes/" + marca + "/models/" + modelo + "/years/" + anyo + "/engines/" + fuel + "/gear-boxes/" + cambio + "/body-types";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);
        return safeExtract(response.getBody(), "body_type_value","carrocerias");
    }

    public List<VersionDTO> getVersion(String marca, String modelo, String anyo, String fuel, String cambio, String carroceria) {
        String url = baseUrl + "/discover/makes/" + marca + "/models/" + modelo + "/years/" + anyo + "/engines/" + fuel + "/gear-boxes/" + cambio + "/body-types/" + carroceria + "/versions";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");

            List<VersionDTO> result = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode item : data) {
                    String id = item.get("vehicle_id").asText();
                    String value = item.get("version_name").asText();
                    result.add(new VersionDTO(id, value));
                }
            }
            return result;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("argument \"content\" is null")) {
                return new ArrayList<>();
            }
            throw new RuntimeException("Error al obtener versiones", e);
        }
    }

    public List<String> getEquipamiento(String vehicleId) {
        String url = baseUrl + "/cars/" + vehicleId + "/carspecs";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode standard = root.path("data").path("standard");

            List<String> result = new ArrayList<>();
            if (standard.isArray()) {
                for (JsonNode section : standard) {
                    String category = section.path("category").asText();
                    JsonNode items = section.path("items");
                    if (items != null && items.isArray()) {
                        result.add("[" + category + "]");
                        for (JsonNode item : items) {
                            result.add("- " + item.asText());
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener equipamiento", e);
        }
    }

    public String leerJato(String idFicha  ) throws JsonProcessingException {

        if (idFicha == null || idFicha.isEmpty()) {
            log.error("ID de ficha no proporcionado para asignar workflow.");
            return "" ;
        }

        System.out.println("Leyendo el campo Rgpd " );
        String accessToken = vStockService.getToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth( accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String valorJato = "" ;

        try {
            String urlWf = URL_BASE_FICHA + idFicha +"/tab/"+ TAB_JATO ;

            ResponseEntity<String> response = restTemplate.exchange(urlWf, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("No se pudo obtener el dato del campo RGPD : {}", response.getStatusCode());
                return "" ;
            }
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            for (JsonNode camposStock : rootNode) {
                String nameJato = camposStock.get("name").asText();
                if (nameJato.equals("Codigo Jato Inv") == false ) {
                    continue ;
                }
                if ( camposStock.path("tabItemConfigInput").path("cardTabItemInstance").get("value") == null ) {  // "tabItemConfigVariable"
                    valorJato = "" ;
                } else {
                    valorJato = camposStock.path("tabItemConfigInput").path("cardTabItemInstance").get("value").asText() ;
                }
            }
        } catch (Exception e) {
            log.error("Error en asignarWorkflow: {}", e.getMessage());
            return "" ;
        }
        return valorJato  ;
    }

//    public JatoInfoDTO obtenerInfoJato(String idFicha) throws JsonProcessingException {
//    if (idFicha == null || idFicha.isEmpty()) {
//        log.error("ID de ficha no proporcionado para asignar workflow.");
//        return null;
//    }
//
//    String accessToken = vStockService.getToken();
//    HttpHeaders headers = new HttpHeaders();
//    headers.setBearerAuth(accessToken);
//    HttpEntity<String> entity = new HttpEntity<>(headers);
//
//    String valorMarca = "";
//    String valorModelo = "";
//
//    try {
//        String urlWf = URL_BASE_FICHA + idFicha + "/tab/" + TAB_DAT_VEHI;
//        ResponseEntity<String> response = restTemplate.exchange(urlWf, HttpMethod.GET, entity, String.class);
//        if (!response.getStatusCode().is2xxSuccessful()) {
//            log.error("No se pudo obtener el dato del campo JATO: {}", response.getStatusCode());
//            return null;
//        }
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode rootNode = objectMapper.readTree(response.getBody());
//
//        for (JsonNode camposStock : rootNode) {
//            String name = camposStock.path("name").asText();
//            System.out.println(name);
//            JsonNode valueNode = camposStock.path("tabItemConfigInput").path("cardTabItemInstance").get("value");
//
//            if (valueNode == null || valueNode.isNull()) continue;
//            String value = valueNode.asText();
//
//            switch (name) {
//                case "Matricula":
//                    valorMarca = value;
//                    break;
//////                case "Modelo Inv":
//////                    valorModelo = value;
//////                    break;
//            }
//        }
//
//        } catch (Exception e) {
//            log.error("Error al leer datos Jato: {}", e.getMessage());
//            return null;
//        }
//        return new JatoInfoDTO(valorMarca, valorModelo);
//    }

}
