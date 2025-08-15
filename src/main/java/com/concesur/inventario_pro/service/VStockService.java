package com.concesur.inventario_pro.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.concesur.inventario_pro.config.AppConfig;
import com.concesur.inventario_pro.dto.ReservaAlDTO;
import com.concesur.inventario_pro.dto.VStockResponseDTO;
import com.concesur.inventario_pro.entity.VStockEntity;
import com.concesur.inventario_pro.repository.VStockRepository;
import com.concesur.inventario_pro.utils.Utils;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Data
@RequiredArgsConstructor
@Slf4j
public class VStockService {
    private final VStockRepository vStockRepository;
    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final Utils utils;
    private static final String CLIENT_ID_AL = "iGCWc8ZzcqrnTAmVwA0kBoHZHUwmkjuP";
    private static final String CLIENT_SECRET_AL = "LkQqwMUueLQ43suN";
    private static final String CODIGO_SI = "1745318473623";
    public String publicadoInvTab;
    private String currentToken = null;
    private String currentTokenAL = null;
    private Long tokenExpirationTime = 0L;
    private String user;
    private String password;
    private String urlToken;
    private int expiresIn = 0;
    @Value("${app.go.publicado.detail}")
    public String urlPublicadoDetail;
    @Value("${app.go.publicado.editar}")
    public String urlPublicadoEditar;
    @Value("${app.autoline.token}")
    private String urlTokenAl;
    @Value("${app.autoline.reserve}")
    private String reservaUrlTemplate;
    private String obtenerVehicleIdTab;
    private String obtenerResponsableReservaTab;

    public void recargarVariables() {
        this.publicadoInvTab = appConfig.getPublicadoInvTab();
        this.user = appConfig.getUser();
        this.password = appConfig.getPassword();
        this.urlToken = appConfig.getUrlToken();
        this.obtenerVehicleIdTab = appConfig.getObtenerVehicleIdTab();
        this.obtenerResponsableReservaTab = appConfig.getObtenerResponsableReservaTab();
    }

    @PostConstruct
    private void init() {
        recargarVariables();
    }

    public List<VStockResponseDTO> obtenerStockFiltrado() {
        return vStockRepository.findStockDisponible()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public VStockResponseDTO obtenerVehiculoPorId(Long id) {
        VStockEntity entity = vStockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con fichaId: " + id));
        return toDto(entity);
    }

    private VStockResponseDTO toDto(VStockEntity stock) {
        return VStockResponseDTO.builder()
                .fichaId(stock.getFichaId())
                .workflowId(stock.getWorkflowId())
                .workflowEstado(stock.getWorkflowEstado())
                .modelo(stock.getModelo())
                .marca(stock.getMarca())
                .version(stock.getVersion())
                .matricula(stock.getMatricula())
                .kms(stock.getKms())
                .color(stock.getColor())
                .interior(stock.getInterior())
                .transmision(stock.getTransmision())
                .fechaMatric(parseDate(stock.getFechaMatric()))
                .origen(stock.getOrigen())
                .precioBase(stock.getPrecioBase())
                .pvp(stock.getPvp())
                .publicado(stock.getPublicado())
                .fechaPublicado(stock.getFechaPublicado())
                .codigoJato(stock.getCodigoJato())
                .ubicacion(stock.getUbicacion())
                .fechaFacturaCompra(parseDate(stock.getFechaFacturaCompra()))
                .vehicleStockId(stock.getVehicleStockId())
                .build();
    }

    @SuppressWarnings("rawtypes")
    public String getToken() {
        long expiresInSeconds = 0L;

        // Verificar si el token aún es válido antes de solicitar uno nuevo
        long tiempoRestante = tokenExpirationTime > 0 ? tokenExpirationTime - Instant.now().getEpochSecond() : 0;
        if (tiempoRestante > 60) {
            log.info("Usando token vigente, expira en: {} segundos.", tiempoRestante);
            return currentToken;
        }

        log.info("Solicitando nuevo token...");

        Map<String, String> payload = Map.of(
                "userName", user,
                "password", password
        );

        try {
            HttpEntity<String> request = createRequestEntity(payload, false);
            ResponseEntity<Map> response = restTemplate.exchange(urlToken, HttpMethod.POST, request, Map.class);

            log.info("Respuesta del POST: {} {}", response.getStatusCode(), response.toString());

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                currentToken = (String) response.getBody().get("access_token");
                expiresIn = Integer.parseInt(response.getBody().get("expires_in").toString());

                expiresInSeconds = expiresIn / 1000;
                tokenExpirationTime = Instant.now().plusSeconds(expiresInSeconds - 60).getEpochSecond();

                long tiempoRestanteCalculado = tokenExpirationTime - Instant.now().getEpochSecond();
                log.info("Nuevo token obtenido, expira en aproximadamente: {} segundos.", tiempoRestanteCalculado);
                return currentToken;
            } else {
                log.error("No se recibió un access_token en la respuesta. Cuerpo de la respuesta: {}", response.getBody());
                return null;
            }
        } catch (JsonProcessingException e) {
            log.error("Error al convertir JSON: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error al hacer la solicitud al endpoint de autenticación: {}", e.getMessage());
            return null;
        }
    }

    public HttpHeaders createHeaders(boolean withAuth) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (withAuth) {
            String token = getToken();
            if (token != null) {
                headers.setBearerAuth(token);
            } else {
                log.error("No se pudo obtener un token de autenticación.");
                throw new RuntimeException("No se pudo obtener un token de autenticación.");
            }
        }
        return headers;
    }

    public HttpEntity<String> createRequestEntity(Object payload, boolean withAuth) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload = objectMapper.writeValueAsString(payload);
        return new HttpEntity<>(jsonPayload, createHeaders(withAuth));
    }

    public void actualizarPublicaciones(VStockResponseDTO publicaciones) {
        String idFicha = String.valueOf(publicaciones.getFichaId());
        String valorSiNo = publicaciones.getPublicado();
        String fecha = publicaciones.getFechaPublicado();

        if (fecha != null && !fecha.isEmpty()) {
            LocalDate fechaOriginal = parseFechaFlexible(fecha);
            if (fechaOriginal != null) {
                fecha = fechaOriginal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else {
                log.warn("❗ No se pudo parsear la fecha '{}'", fecha);
                fecha = null;
            }
        }
        actualizarPublicado(idFicha, valorSiNo, fecha);
    }

    private LocalDate parseFechaFlexible(String fecha) {
        DateTimeFormatter[] formatos = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };
        for (DateTimeFormatter formatter : formatos) {
            try {
                return LocalDate.parse(fecha, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    public void actualizarPublicado(String idFicha, String valorSiNo, String fechaPublicado) {
        try {
            Map<String, String> codigosSiNo = new HashMap<>();
            codigosSiNo.put("Si", "1745318473623");
            codigosSiNo.put("No", "1745318480587");

            String codigo = codigosSiNo.get(valorSiNo);
            if (codigo == null) {
                log.warn("❗ Valor '{}' no reconocido. Solo se permiten 'Si' o 'No'.", valorSiNo);
                return;
            }

            Map<String, String> datos = new HashMap<>();
            datos.put("Publicar Inv", codigo);
            if(codigo.equals(CODIGO_SI) && fechaPublicado != null) {
                datos.put("Fecha publicado Inv", fechaPublicado);
            }
            String urlGet = String.format(urlPublicadoDetail, idFicha, publicadoInvTab);
            String urlPost = String.format(urlPublicadoEditar, idFicha, publicadoInvTab);

            procesarTabItems(urlGet, urlPost, datos);

        } catch (Exception e) {
            log.error("Error al actualizar publicado para ficha {}: {}", idFicha, e.getMessage(), e);
        }
    }

    public void actualizarJato(String idFicha, String codigoJato) {
        try {
            Map<String, String> datos = new HashMap<>();
            datos.put("Codigo Jato Inv", codigoJato);


            String urlGet = String.format(urlPublicadoDetail, idFicha, publicadoInvTab);
            String urlPost = String.format(urlPublicadoEditar, idFicha, publicadoInvTab);

            procesarTabItems(urlGet, urlPost, datos);

        } catch (Exception e) {
            log.error("Error al actualizar publicado para ficha {}: {}", idFicha, e.getMessage(), e);
        }
    }

    public String procesarTabItems(String urlGet, String urlPost, Map<String, String> datosFactura) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(urlGet, HttpMethod.GET, createRequestEntity(null, true), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Error obteniendo detalle: {}", response.getStatusCode());
                return "Error obteniendo detalle en " + urlGet;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode tabItems = (ArrayNode) objectMapper.readTree(response.getBody());
            ArrayNode arrayToSend = objectMapper.createArrayNode();

            for (JsonNode item : tabItems) {
                String campo = item.path("name").asText();
                log.debug("Campo detectado en tabItems: '{}'", campo);
                String idNode = "";
                String nuevoValor = "" ;

                String tipo = item.path("typeItem").asText();

                JsonNode cardTabItemInstanceNode = null;


                if ("LIST".equals(tipo)) {
                    cardTabItemInstanceNode = item.path("tabItemConfigList").path("cardTabItemInstance");
                } else if ("INPUT".equals(tipo)) {
                    cardTabItemInstanceNode = item.path("tabItemConfigInput").path("cardTabItemInstance");
                }

                if (cardTabItemInstanceNode != null && cardTabItemInstanceNode.isObject()) {
                    idNode = cardTabItemInstanceNode.path("id").asText();
                    nuevoValor = cardTabItemInstanceNode.path("value").asText();
                }
                if (datosFactura.containsKey(campo)) {
                    nuevoValor = datosFactura.get(campo);
                }
                if(nuevoValor != null && !nuevoValor.isEmpty()) {

                    ObjectNode objetoFinal = objectMapper.createObjectNode();
                    objetoFinal.put("id", idNode);
                    ObjectNode tabItem = objectMapper.createObjectNode();
                    tabItem.put("id", item.path("id").asLong());
                    tabItem.put("tabId", item.path("tabId").asLong());
                    tabItem.put("typeItem", item.path("typeItem").asText());
                    tabItem.put("orderNumber", item.path("orderNumber").asInt());
                    tabItem.put("name", item.path("name").asText());

                    objetoFinal.set("tabItem", tabItem);

                    objetoFinal.put("value", nuevoValor);

                    arrayToSend.add(objetoFinal);
                }
            }

            if (arrayToSend.isEmpty()) {
                log.warn("No se encontró ningún campo para actualizar en {}", urlPost);
                return "No se encontró ningún campo para actualizar en " + urlPost;
            }

            String payload = objectMapper.writeValueAsString(arrayToSend);
            log.info("Payload final enviado a {}:\n{}", urlPost, payload);

            HttpEntity<String> updateRequest = new HttpEntity<>(payload, createHeaders(true));
            ResponseEntity<String> postResponse = restTemplate.exchange(urlPost, HttpMethod.POST, updateRequest, String.class);

            if (postResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Campos actualizados correctamente en {}", urlPost);
                return "Campos actualizados en " + urlPost;
            } else {
                log.warn("Error actualizando campos en {}: {}", urlPost, postResponse.getStatusCode());
                return "Error actualizando campos en " + urlPost + ": " + postResponse.getStatusCode();
            }

        } catch (Exception e) {
            log.error("Error procesando tabItems: {}", e.getMessage(), e);
            return "Error procesando tabItems en " + urlGet + ": " + e.getMessage();
        }
    }

    public String getTokenAL(String url, String user, String password) throws JsonProcessingException {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(user, password);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        RestTemplate restTemplate = new RestTemplate();
        String body = "grant_type=client_credentials";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);


        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error obteniendo el token de autenticación de autoline");
        }

        currentTokenAL = response.getBody().get("access_token").toString();

        return currentTokenAL;
    }

    public void reservarVehiculo(String vehicleId, ReservaAlDTO dto, String tokenUrl, String user, String password) throws JsonProcessingException {
        String accessToken = getTokenAL(tokenUrl, user, password);
        String urlReserva = String.format(reservaUrlTemplate, vehicleId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReservaAlDTO> reservaRequest = new HttpEntity<>(dto, headers);

        ResponseEntity<String> reservaResponse = restTemplate.exchange(urlReserva, HttpMethod.POST, reservaRequest, String.class);

        if (!reservaResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error al reservar vehículo: " + reservaResponse.getStatusCode());
        }
    }
    private LocalDate parseDate(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return null;

        String[] patrones = {
                "dd/MM/yyyy",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
        };

        for (String patron : patrones) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(patron);
                return LocalDate.parse(fechaStr.substring(0, patron.length()), fmt);
            } catch (DateTimeParseException ignored) {}
        }

        log.warn("No se pudo parsear la fecha '{}'", fechaStr);
        return null;
    }

    public Map<String, String> obtenerDatosPublicadoInv(Long fichaId) {
        Map<String, String> resultado = new HashMap<>();
        try {
            String url   = String.format(urlPublicadoDetail, fichaId, publicadoInvTab);
            String token = getToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            MultiValueMap<String, String> datos = utils.leerTabItems(url, entity);

            String ref = datos.getFirst("Ref anuncio");
            resultado.put("refAnuncio", StringUtils.hasText(ref) ? ref.trim() : "");

            String colorInv = datos.getFirst("Color Inv");
            String colorFinal = "";
            if (StringUtils.hasText(colorInv)) {
                if (colorInv.contains("-")) {
                    String[] partes = colorInv.split("-", 2);
                    colorFinal = partes[0].trim();
                } else if (colorInv.trim().length() > 1) {
                    colorFinal = colorInv.trim();
                }
            }
            resultado.put("codigoColor", StringUtils.hasText(colorFinal) ? colorFinal.trim() : "");

            String codigoTienda = datos.getFirst("Tiendas");
            if(StringUtils.hasText(codigoTienda) && codigoTienda.length() > 1) {
                resultado.put("tiendas", StringUtils.hasText(codigoTienda) ? codigoTienda.trim() : "");
            }
        } catch (Exception e) {
            log.warn("Error leyendo datos tab 23 para ficha {}: {}", fichaId, e.getMessage());
            resultado.putIfAbsent("refAnuncio", "");
            resultado.putIfAbsent("codigoColor", "");
            resultado.putIfAbsent("tiendas", "");
        }
        return resultado;
    }

}
