package com.concesur.inventario_pro.service;

import java.net.URI;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.concesur.inventario_pro.dto.CambioEstadoAnuncioRequestDTO;
import com.concesur.inventario_pro.utils.Utils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GestionInventarioPro {

    private final RestTemplate restTemplate;
    @Qualifier("restTemplateSinRedirect")
    private final RestTemplate restTemplateSinRedirect;
    private final VStockService vStockService;
    private final Utils utils;
    @Value("${app.inventarioPRO.user}")
    private String usuarioInventario;
    @Value("${app.inventarioPRO.pass}")
    private String passwordInventario;
    @Value("${app.inventarioPro.gestion}")
    private String urlBaseGestionInventario;
    @Value("${app.go.fotos.tab}")
    private String tabFotos;
    @Value("${app.go.url.adjuntos}")
    private String urlGoAdjuntos;
    @Value("${app.inventarioPro.url.addfoto}")
    private String urladdFoto;
    @Value("${app.go.url.download.Adjuntos}")
    private String urlGoDownloadAjuntos;
    @Value("${app.inventarioPro.url.AjaxControlTiempoFotos}")
    private String urlAjaxControlTiempo;
    @Value("${app.inventarioPro.url.signedS3}")
    private String urlInventarioProSignedS3;
    @Value("${app.inventarioPro.urlPublicarAnuncio}")
    private String urlPublicarAnuncio;
    @Value("${app.inventarioPro.urlBusquedaAnuncio}")
    private String urlBusquedaAnuncio;
    @Value("${app.inventarioPro.urlCambioEstadoAnuncio}")
    private String urlCambioEstadoAnuncio;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public String autenticarInventarioPro() {
        String urlLogin = urlBaseGestionInventario.trim() + "login/entrar";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("user",  usuarioInventario);
        requestBody.add("pass",  passwordInventario);
        HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(urlLogin, HttpMethod.POST, requestEntity, Map.class) ;
        //System.out.println( response.toString());

        String set_cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        return set_cookie;
    }

    /** Devuelve el código seleccionado para cualquier LIST (root.value o cardTabItemInstance.value). */
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

    public static void setFechaMatriculacionFromTimestamp(String timestampStr, MultiValueMap<String, Object> formData) {
        long timestamp = Long.parseLong(timestampStr);
        Date fecha = new Date(timestamp);
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);

        formData.add("matriculacion_day", String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
        formData.add("matriculacion_month", String.valueOf(cal.get(Calendar.MONTH) + 1));
        formData.add("matriculacion_year", String.valueOf(cal.get(Calendar.YEAR)));
    }

    @SuppressWarnings("unchecked")
    public String crearFormularioAvanzado(String codJato, String idFicha) {

        String cookieSessionString = autenticarInventarioPro();

        if (cookieSessionString == null || cookieSessionString.startsWith("Error")) {
            log.error("No se pudo autenticar en InventarioPRO");
            return "No se pudo autenticar en InventarioPRO: " + cookieSessionString;
        }

        try {
            HttpHeaders headersConCookie = new HttpHeaders();
            headersConCookie.set("Cookie", cookieSessionString);
            headersConCookie.set("referer", urlBaseGestionInventario + "coche/publicaravanzado");
            headersConCookie.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> jatoPayload = new LinkedMultiValueMap<>();
            jatoPayload.add("vehicle_id", codJato);

            ResponseEntity<Map> responseJato = restTemplate.exchange(urlBaseGestionInventario + "/ajax/getEnriquecimientoJato", HttpMethod.POST,
                    new HttpEntity<>(jatoPayload, headersConCookie),
                    Map.class
            );

            if (!responseJato.getStatusCode().is2xxSuccessful()) {
                log.error("No se pudieron leer los datos del cliente: {}", responseJato.getStatusCode());
                return "Error leyendo datos de JATO: " + responseJato.getStatusCode();
            }

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            responseJato.getBody().forEach((k, v) -> formData.add((String) k, v));
            Object descripcionEquipamientos = responseJato.getBody().get("descripcion_equipamientos");
            if (descripcionEquipamientos != null) {
                formData.add("equipamiento_serie", descripcionEquipamientos);
            }
            String urlGet23 = String.format(vStockService.getUrlPublicadoDetail(), idFicha, "23");
            String urlGet20 = String.format(vStockService.getUrlPublicadoDetail(), idFicha, "20");
            String accessToken = vStockService.getToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            MultiValueMap<String, String> datosStock20 = utils.leerTabItems(urlGet20, entity);
            MultiValueMap<String, String> datosStock23 = utils.leerTabItems(urlGet23, entity);

            String matricula = datosStock20.getFirst("Matricula");
            formData.add("matricula", matricula);
            String referencia = datosStock20.getFirst("Comision");
            if(!StringUtils.hasText(referencia)) {
                referencia = matricula;
            }
            formData.add("referencia",  referencia);
            String codigoTienda = datosStock23.getFirst("Tiendas");
            if(StringUtils.hasText(codigoTienda) && codigoTienda.length() > 1) {
                codigoTienda = codigoTienda.split("-")[1];
            }
            formData.add("location_id", codigoTienda);
            String fechaMatriculacion = datosStock20.getFirst("Fecha de Matriculacion");
            if (fechaMatriculacion != null && fechaMatriculacion.matches("\\d+")) {
                setFechaMatriculacionFromTimestamp(fechaMatriculacion, formData);
            }

            String num_bastidor = datosStock20.getFirst("num_bastidor");
            formData.add("num_bastidor", num_bastidor);
            String kilometros =datosStock23.getFirst("Kilometros manual");
            if(!StringUtils.hasText(kilometros)) {
                kilometros = "99999999";
            }
            formData.add("kilometros", kilometros);
            String precio = datosStock20.getFirst("PVP");
            if(StringUtils.hasText(precio) && !utils.esPrecioValido(precio)) {
                precio = "99999999";
            }
            formData.add("precio", precio);
            String adSubtype = "50";
            formData.add("ad_subtype_id", adSubtype);
            String descripcion = datosStock23.getFirst("Especificaciones");
            formData.add("descripcion", descripcion);

            String enlaceVideo = datosStock23.getFirst("ENLACE_YOUTUBE");
            if(!StringUtils.hasText(enlaceVideo)) {
                enlaceVideo = "";
            }
            String codigoColor = datosStock23.getFirst("Color Inv");

            if(StringUtils.hasText(codigoColor) && codigoColor.length() > 1) {
                codigoColor = codigoColor.split("-")[1];
            }
            formData.add("color_id", codigoColor);

            //crearFormularioBase(formData, datosCoche);
            Map<String, String> camposRequeridos = new HashMap<>();
            camposRequeridos.put("Comision", referencia);
            camposRequeridos.put("Tiendas", codigoTienda);
            camposRequeridos.put("Fecha de Matriculacion", fechaMatriculacion);
            camposRequeridos.put("Matricula", matricula);
            camposRequeridos.put("Kilometros manual", kilometros);
            camposRequeridos.put("PVP", precio);
            camposRequeridos.put("ad_subtype_id", adSubtype);
            camposRequeridos.put("Especificaciones", descripcion);
            camposRequeridos.put("Color Inv", codigoColor);
            camposRequeridos.put("Jato", codJato);

            Optional<String> errorValidacion = utils.validarCamposObligatorios(camposRequeridos);
            if (errorValidacion.isPresent()) {
                return errorValidacion.get();
            }

            ResponseEntity<String> responseFinal = restTemplateSinRedirect.exchange(urlBaseGestionInventario + "/coche/guardaravanzado", HttpMethod.POST,
                    new HttpEntity<>(formData, headersConCookie), String.class);

            String referenciaAnuncio = "";
        // Verifica si la respuesta es un 302 (Found)
            if (responseFinal.getStatusCode() == HttpStatus.FOUND) {
                String location = responseFinal.getHeaders().getFirst("Location");
                log.info("Redirección detectada. Location: {}", location);

                referenciaAnuncio = utils.extraerReferenciaDesdeLocation(location);
                Map<String, String> datos = new HashMap<>();
                datos.put("Ref anuncio", referenciaAnuncio);
                String urlGet = String.format(vStockService.getUrlPublicadoDetail(), idFicha, vStockService.publicadoInvTab);
                String urlPost = String.format(vStockService.getUrlPublicadoEditar(), idFicha, vStockService.publicadoInvTab);
                vStockService.procesarTabItems(urlGet, urlPost, datos);
                if (!referenciaAnuncio.isBlank()) {
                    adjuntaFoto(referenciaAnuncio, idFicha);
                    return "Alta realizada correctamente. Ref: " + referenciaAnuncio;
                } else {
                    return "Redirección sin referencia válida. No se creó el anuncio.";
                }
            } else {
                log.warn("Respuesta sin redirección. Código: {}", responseFinal.getStatusCode());
                return "No se pudo crear el anuncio. Código recibido: " + responseFinal.getStatusCode();
            }

        } catch (JsonProcessingException e) {
            log.error("Error procesando JSON en crearFormularioAvanzado: {}", e.getMessage(), e);
            return "Error procesando JSON";
        } catch (RestClientException e) {
            log.error("Error en llamada REST en crearFormularioAvanzado: {}", e.getMessage(), e);
            return "Error en comunicación con servicios externos";
        } catch (Exception e) {
            log.error("Error inesperado en crearFormularioAvanzado: {}", e.getMessage(), e);
            return "Error inesperado al crear el formulario";
        }
    }

//    private void crearFormularioBase(MultiValueMap<String, Object> formData, MultiValueMap<String, String> datosStock20, MultiValueMap<String, String> datosStock23) {
//        formData.add("referencia", datosStock20.getReferencia());
//        formData.add("location_id", datosCoche.getLocationId());
//        formData.add("matriculacion_day", datosCoche.getMatriculacionDay());
//        formData.add("matriculacion_month", datosCoche.getMatriculacionMonth());
//        formData.add("matriculacion_year", datosCoche.getMatriculacionYear());
//        formData.add("matricula", datosCoche.getMatricula());
//        formData.add("num_bastidor", datosCoche.getNumBastidor());
//        formData.add("kilometros", datosCoche.getKilometros());
//        formData.add("precio", datosCoche.getPrecio());
//        formData.add("ad_subtype_id", datosCoche.getAdSubtypeId());
//        formData.add("descripcion", datosCoche.getDescripcion());
//        formData.add("color_id", datosCoche.getColorId());
//    }

    public String postFile(String filename, byte[] someByteArray, String refAnuncio, String cookie) throws JsonProcessingException {
        String uuid = "";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("cookie", cookie);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.set("adId", String.valueOf(refAnuncio));
        System.out.println("Subiendo archivo: " + filename + " (" + someByteArray.length + " bytes)");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body,headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(urlInventarioProSignedS3, HttpMethod.POST, requestEntity, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            uuid = rootNode.path("uuid").asText();
            String urlFirmada = rootNode.path("url").asText();
            System.out.println(urlFirmada);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<byte[]> putRequest = new HttpEntity<>(someByteArray,headers);
            URI uriFirma = new URI(urlFirmada);
            ResponseEntity<String> putResponse = restTemplate.exchange(uriFirma, HttpMethod.PUT, putRequest, String.class);

            System.out.println("PUT S3 Status: " + putResponse.getStatusCode());

        } catch (HttpClientErrorException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error al subir archivo: " + e.getMessage());
        }

        return uuid;
    }

    private void adjuntaFoto(String refAnuncio, String idFicha) {
        String cookie = autenticarInventarioPro();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("cookie", cookie);
        String urlGet = String.format(urlGoAdjuntos, idFicha, tabFotos);
        String token = vStockService.getToken();

        HttpHeaders headersGo = new HttpHeaders();
        headersGo.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headersGo);

        try {
            ResponseEntity<String> response = restTemplate.exchange(urlGet, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("No se pudo obtener adjuntos desde GO.");
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());
            int contadorFotos = 0;
            for (JsonNode adjunto : root) {
                //adjunto.path("attachments");
                String rama = adjunto.path("templateAttachmentItem").path("name").asText();
                if(!rama.equalsIgnoreCase("Fotos_web"))
                    continue;
                JsonNode files = adjunto.path("attachments");
                for(JsonNode imagenes : files) {
                    String nombreArchivo = imagenes.path("name").asText();
                    long idAdjunto = imagenes.path("id").asLong();
                    System.out.println("idImagenes: "+idAdjunto);
                    String urlDescarga = String.format(urlGoDownloadAjuntos, idFicha, idAdjunto);
                    ResponseEntity<String> responseImagen = restTemplate.exchange(urlDescarga, HttpMethod.GET, new HttpEntity<>(headersGo),
                            String.class
                    );
                    JsonNode datosImagen = objectMapper.readTree(responseImagen.getBody());

                    String base64 = datosImagen.path("content").asText();
                    int coma = base64.indexOf(',');
                    if (coma > 0) {
                        base64 = base64.substring(coma + 1);
                    }
                    byte[] imagenBytes;
                    imagenBytes = Base64.getDecoder().decode(base64);

                    contadorFotos++;

                    String uuid = postFile(nombreArchivo, imagenBytes, refAnuncio, cookie);
                    if (uuid == null || uuid.isEmpty()) {
                        log.warn("UUID vacío al subir archivo '{}'. Se omite.", nombreArchivo);
                        continue;
                    }

                    MultiValueMap<String, String> bodyFoto = new LinkedMultiValueMap<>();
                    bodyFoto.add("adId", refAnuncio);
                    bodyFoto.add("uuid", uuid);
                    if(contadorFotos==1) {
                        bodyFoto.add("principal", String.valueOf(1));
                    }
                    bodyFoto.add("orden", String.valueOf(contadorFotos));

                    ResponseEntity<String> responseAddFoto = restTemplate.exchange(urladdFoto, HttpMethod.POST, new HttpEntity<>(bodyFoto, headers),
                            String.class
                    );
                    System.out.println("Respuesta addFoto:\n" + responseAddFoto.getBody());
                    if (!responseAddFoto.getStatusCode().is2xxSuccessful()) {
                        log.error("No se pudo obtener adjuntos desde GO.: " + responseAddFoto.getStatusCode());
                        return;
                    }
                    MultiValueMap<String, String> bodyContador = new LinkedMultiValueMap<>();
                    bodyContador.add("anuncio_id", refAnuncio);
                    bodyContador.add("tiempo", "662");
                    bodyContador.add("total_fotos", String.valueOf(contadorFotos));

                    ResponseEntity<String> responseAjaxControlTiempo =  restTemplate.exchange(urlAjaxControlTiempo, HttpMethod.POST, new HttpEntity<>(bodyContador, headers),
                            String.class
                    );
                    if (!responseAjaxControlTiempo.getStatusCode().is2xxSuccessful()) {
                        log.error("No se pudo obtener adjuntos desde GO.: " + responseAjaxControlTiempo.getStatusCode());
                        return;
                    }
                    publicarFoto(refAnuncio, headers);
                }

            }
        } catch (Exception e) {
            log.error("Error al adjuntar fotos: {}", e.getMessage(), e);
        }
    }

    private void publicarFoto(String refAnuncio, HttpHeaders headers) {
        MultiValueMap<String, String> publicarAnuncio = new LinkedMultiValueMap<>();
        publicarAnuncio.add("adId", refAnuncio);
        publicarAnuncio.add("destacar_web", String.valueOf(0));
        ResponseEntity<String> responsePublicarFoto =  restTemplate.exchange(urlPublicarAnuncio+refAnuncio, HttpMethod.POST, new HttpEntity<>(publicarAnuncio, headers),
                String.class
            );
        if (!responsePublicarFoto.getStatusCode().is2xxSuccessful()) {
            log.error("No se ha podido publicar el anuncio: " + responsePublicarFoto.getStatusCode());
            return;
        }
    }

    public Map<String, Object> buscarVehiculo(String idFicha, CambioEstadoAnuncioRequestDTO datosAnuncio) throws RestClientException, JsonProcessingException {
        String urlGet20 = String.format(vStockService.getUrlPublicadoDetail(), idFicha, "20");
        String accessToken = vStockService.getToken();
        String refAnuncio = "";
        // Cabeceras con el token de autorización de go
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        MultiValueMap<String, String> datosStock20 = utils.leerTabItems(urlGet20, entity);
        String matricula = datosStock20.getFirst("Matricula");


        try {
            String cookie = autenticarInventarioPro();
            HttpHeaders headersInventario = new HttpHeaders();
            headersInventario.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headersInventario.add("cookie", cookie);
            HttpEntity<Void> entityInventario = new HttpEntity<>(headersInventario);

            ResponseEntity<String> response = restTemplate.exchange(urlBusquedaAnuncio + matricula, HttpMethod.GET, entityInventario, String.class);
            String body = response.getBody();
            if (body != null) {
                refAnuncio = utils.extraerReferenciaDesdeBody(body);
                if(StringUtils.hasText(refAnuncio)) {
                    return cambiarEstado(refAnuncio, headersInventario, datosAnuncio);
                }
            }
        } catch (Exception e) {
            log.error("Error al buscar vehículo: " + e.getMessage());
        }

        return null;
    }

    private Map<String, Object> cambiarEstado(String refAnuncio, HttpHeaders header, CambioEstadoAnuncioRequestDTO datosAnuncio) {
        try {
            MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
            payload.add("anuncio_id", refAnuncio);

            if (datosAnuncio != null) {
                payload.add("estado", utils.comprobarNulo(datosAnuncio.getEstado()));
                payload.add("nombre", ""/*utils.comprobarNulo(datosAnuncio.getNombre())*/);
                payload.add("apellidos", ""/*utils.comprobarNulo(datosAnuncio.getApellidos())*/);
                payload.add("telefono", ""/*utils.comprobarNulo(datosAnuncio.getTelefono())*/);
                payload.add("email", ""/*utils.comprobarNulo(datosAnuncio.getEmail())*/);
                payload.add("pago_metodo","" /*utils.comprobarNulo(datosAnuncio.getPago_metodo())*/);
                payload.add("pago_cantidad", ""/*utils.comprobarNulo(datosAnuncio.getPago_cantidad())*/);
                payload.add("pago_transaccion","" /*utils.comprobarNulo(datosAnuncio.getPago_transaccion())*/);
                payload.add("comentarios", ""/*utils.comprobarNulo(datosAnuncio.getComentarios())*/);
            }

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(payload, header);

            ResponseEntity<String> response = restTemplate.exchange(urlCambioEstadoAnuncio, HttpMethod.POST, requestEntity,
                    String.class
            );


            String body = response.getBody();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);

            int status = root.path("status").asInt(-1);
            String mensaje = root.path("response").path("mensaje").asText("Sin mensaje");

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", status);
            resultado.put("response", Map.of(
                    "mensaje", mensaje + " ID: " + refAnuncio
            ));

            return resultado;

        } catch (Exception e) {
            log.error("Error al cambiar el estado del anuncio " + refAnuncio + ": " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", -1);
            error.put("response", Map.of(
                    "mensaje", "Error al cambiar el anuncio: " + e.getMessage()
            ));
            return error;
        }
    }
}
