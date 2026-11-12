package com.tenisahora.arquitectura;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reglas de arquitectura y de código limpio del backend. Leen los fuentes de src/main/java: si alguien las rompe, el
 * test falla y dice qué archivo y por qué. Son las reglas que documentan los package-info.
 */
class ArquitecturaTest {

    private static final Path FUENTES = Path.of("src/main/java/com/tenisahora");
    private static final int MAXIMO_LINEAS_POR_METODO = 25;
    private static final int MAXIMO_LINEAS_POR_CLASE = 350;

    private static final Pattern PAQUETE = Pattern.compile("^package com\\.tenisahora\\.?([\\w.]*);", Pattern.MULTILINE);
    private static final Pattern IMPORT = Pattern.compile("^import com\\.tenisahora\\.([\\w.]+);", Pattern.MULTILINE);

    /** Un archivo fuente: en qué paquete está y qué paquetes del proyecto importa. */
    record Fuente(String ruta, String paquete, Set<String> imports, String texto) {
        /** "modules.socio" para lo que está en modules/socio; "security", "shared"... para el resto. */
        String nodo() {
            return nodoDe(paquete);
        }
    }

    static String nodoDe(String paquete) {
        String[] partes = paquete.split("\\.");
        if (partes.length >= 2 && partes[0].equals("modules")) {
            return partes[0] + "." + partes[1];
        }
        return partes[0];
    }

    static Fuente leer(String ruta, String texto) {
        Matcher paquete = PAQUETE.matcher(texto);
        String nombrePaquete = paquete.find() ? paquete.group(1) : "";
        Set<String> imports = new LinkedHashSet<>();
        Matcher importado = IMPORT.matcher(texto);
        while (importado.find()) {
            imports.add(importado.group(1));
        }
        return new Fuente(ruta, nombrePaquete, imports, texto);
    }

    private static List<Fuente> fuentes() throws IOException {
        List<Fuente> lista = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(FUENTES)) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                lista.add(leer(FUENTES.relativize(archivo).toString().replace('\\', '/'), Files.readString(archivo)));
            }
        }
        return lista;
    }

    // ---------- Quién puede depender de quién ----------

    @Test
    void security_noDependeDeNingunModulo() throws IOException {
        assertThat(importesProhibidos("security", "modules.")).as(
                "security no puede depender de ningún módulo (solo recibe id y rol, o usa un puerto)").isEmpty();
    }

    @Test
    void shared_noDependeDeNingunModulo() throws IOException {
        assertThat(importesProhibidos("shared", "modules.")).as("shared lo usan todos: no puede depender de un módulo").isEmpty();
    }

    @Test
    void infrastructure_noDependeDeNingunModulo() throws IOException {
        assertThat(importesProhibidos("infrastructure", "modules.")).as(
                "infrastructure recibe todo ya armado: no conoce los módulos").isEmpty();
    }


    @Test
    void losModulosDeRol_noDependenDeOtrosModulos() throws IOException {
        for (String rol : List.of("modules.socio", "modules.empleado", "modules.administrador", "modules.instructor")) {
            assertThat(dependenciasHaciaOtrosModulos(rol)).as(
                    rol + " tiene su propia tabla y no puede importar a ningún otro módulo: lo común sale de shared").isEmpty();
        }
    }

    @Test
    void cancha_noDependeDeOtrosModulos_usaPuertos() throws IOException {
        assertThat(dependenciasHaciaOtrosModulos("modules.cancha")).as(
                "cancha define puertos (como ReservasDelRango) y otros módulos los implementan: no puede importar a reserva ni a ninguno").isEmpty();
    }

    @Test
    void insumo_noDependeDeOtrosModulos_usaPuertos() throws IOException {
        assertThat(dependenciasHaciaOtrosModulos("modules.insumo")).as(
                "insumo define puertos (como UsoDeInsumos) y otros módulos los implementan: no puede importar a reserva ni a ninguno").isEmpty();
    }

    private static List<String> dependenciasHaciaOtrosModulos(String modulo) throws IOException {
        List<String> violaciones = new ArrayList<>();
        for (Fuente fuente : fuentes()) {
            if (!fuente.nodo().equals(modulo)) {
                continue;
            }
            for (String importado : fuente.imports()) {
                if (importado.startsWith("modules.") && !nodoDe(importado).equals(modulo)) {
                    violaciones.add(fuente.ruta() + " importa " + importado);
                }
            }
        }
        return violaciones;
    }

    @Test
    void losControllers_noUsanRepositorios_hablanConElService() throws IOException {
        List<String> violaciones = new ArrayList<>();
        for (Fuente fuente : fuentes()) {
            if (!fuente.paquete().endsWith(".controller")) {
                continue;
            }
            fuente.imports().stream().filter(i -> i.contains(".repository.")).forEach(i -> violaciones.add(fuente.ruta() + " importa " + i));
        }
        assertThat(violaciones).as("un controller no toca la base: pasa por el service").isEmpty();
    }

    @Test
    void lasCapasBajas_noConocenAlController() throws IOException {
        List<String> violaciones = new ArrayList<>();
        for (Fuente fuente : fuentes()) {
            boolean capaBaja = fuente.paquete().endsWith(".service") || fuente.paquete().endsWith(".repository")
                    || fuente.paquete().endsWith(".entity");
            if (capaBaja) {
                fuente.imports().stream().filter(i -> i.contains(".controller.")).forEach(i -> violaciones.add(fuente.ruta() + " importa " + i));
            }
        }
        assertThat(violaciones).as("service, repository y entity no dependen del controller").isEmpty();
    }

    @Test
    void entrePaquetes_noHayCiclos() throws IOException {
        Map<String, Set<String>> grafo = grafoDeDependencias(fuentes());

        assertThat(encontrarCiclo(grafo)).as("dos paquetes no pueden depender uno del otro (directa o indirectamente)").isEmpty();
    }

    // ---------- Código limpio ----------

    @Test
    void ningunMetodo_pasaDeLasLineasMaximas() throws IOException {
        List<String> largos = new ArrayList<>();
        for (Fuente fuente : fuentes()) {
            metodosLargos(fuente.texto(), MAXIMO_LINEAS_POR_METODO)
                    .forEach(m -> largos.add(fuente.ruta() + " → " + m));
        }
        assertThat(largos).as("un método de más de " + MAXIMO_LINEAS_POR_METODO
                + " líneas se parte en pasos con nombre").isEmpty();
    }

    @Test
    void ningunaClase_pasaDeLasLineasMaximas() throws IOException {
        List<String> grandes = new ArrayList<>();
        for (Fuente fuente : fuentes()) {
            long lineas = fuente.texto().lines().count();
            if (lineas > MAXIMO_LINEAS_POR_CLASE) {
                grandes.add(fuente.ruta() + " tiene " + lineas + " líneas");
            }
        }
        assertThat(grandes).as("una clase de más de " + MAXIMO_LINEAS_POR_CLASE + " líneas hace demasiado").isEmpty();
    }

    @Test
    void elCodigoDeProduccion_noImprimePorConsola() throws IOException {
        List<String> violaciones = new ArrayList<>();
        for (Fuente fuente : fuentes()) {
            if (fuente.texto().contains("System.out.print") || fuente.texto().contains("printStackTrace")) {
                violaciones.add(fuente.ruta());
            }
        }
        assertThat(violaciones).as("se usa el logger (@Slf4j), no la consola").isEmpty();
    }

    // ---------- Autopruebas: que los detectores de arriba detecten de verdad ----------

    @Test
    void autoprueba_elDetectorDeMetodosLargos_encuentraUnMetodoDeMasDeLoPermitido() {
        String cuerpo = "        int x = 0;\n".repeat(30);
        String clase = "class A {\n    public void largo() {\n" + cuerpo + "    }\n\n    public void corto() {\n        int y = 1;\n    }\n}\n";

        List<String> encontrados = metodosLargos(clase, MAXIMO_LINEAS_POR_METODO);

        assertThat(encontrados).hasSize(1);
        assertThat(encontrados.get(0)).contains("largo()");
    }

    @Test
    void autoprueba_elDetectorDeImports_lePuedeAtribuirUnModuloAUnArchivoDeSecurity() {
        Fuente fuente = leer("security/X.java",
                "package com.tenisahora.security;\nimport com.tenisahora.modules.socio.entity.Socio;\nclass X {}\n");

        assertThat(fuente.nodo()).isEqualTo("security");
        assertThat(fuente.imports()).containsExactly("modules.socio.entity.Socio");
        assertThat(nodoDe("modules.socio.entity.Socio")).isEqualTo("modules.socio");
    }

    @Test
    void autoprueba_elDetectorDeCiclos_encuentraUnCicloYNoInventaUnoDondeNoHay() {
        Map<String, Set<String>> conCiclo = Map.of("a", Set.of("b"), "b", Set.of("c"), "c", Set.of("a"));
        Map<String, Set<String>> sinCiclo = Map.of("a", Set.of("b"), "b", Set.of("c"), "c", Set.of());

        assertThat(encontrarCiclo(conCiclo)).isNotEmpty();
        assertThat(encontrarCiclo(sinCiclo)).isEmpty();
    }

    // ---------- Detectores ----------

    private static List<String> importesProhibidos(String paqueteOrigen, String prefijoProhibido) throws IOException {
        List<String> violaciones = new ArrayList<>();
        for (Fuente fuente : fuentes()) {
            if (!fuente.nodo().equals(paqueteOrigen)) {
                continue;
            }
            fuente.imports().stream().filter(i -> i.startsWith(prefijoProhibido))
                    .forEach(i -> violaciones.add(fuente.ruta() + " importa " + i));
        }
        return violaciones;
    }

    private static Map<String, Set<String>> grafoDeDependencias(List<Fuente> fuentes) {
        Map<String, Set<String>> grafo = new LinkedHashMap<>();
        for (Fuente fuente : fuentes) {
            Set<String> destinos = grafo.computeIfAbsent(fuente.nodo(), k -> new LinkedHashSet<>());
            for (String importado : fuente.imports()) {
                String destino = nodoDe(importado);
                if (!destino.equals(fuente.nodo())) {
                    destinos.add(destino);
                }
            }
        }
        return grafo;
    }

    /** Devuelve el camino de un ciclo si lo hay (por ejemplo [a, b, c, a]), o una lista vacía. */
    static List<String> encontrarCiclo(Map<String, Set<String>> grafo) {
        Set<String> terminados = new LinkedHashSet<>();
        for (String inicio : grafo.keySet()) {
            Deque<String> camino = new ArrayDeque<>();
            List<String> ciclo = visitar(inicio, grafo, camino, terminados);
            if (!ciclo.isEmpty()) {
                return ciclo;
            }
        }
        return List.of();
    }

    private static List<String> visitar(String nodo, Map<String, Set<String>> grafo, Deque<String> camino, Set<String> terminados) {
        if (camino.contains(nodo)) {
            List<String> ciclo = new ArrayList<>(camino.stream().toList().reversed());
            ciclo = ciclo.subList(ciclo.indexOf(nodo), ciclo.size());
            ciclo.add(nodo);
            return ciclo;
        }
        if (terminados.contains(nodo)) {
            return List.of();
        }
        camino.push(nodo);
        for (String vecino : grafo.getOrDefault(nodo, Set.of())) {
            List<String> ciclo = visitar(vecino, grafo, camino, terminados);
            if (!ciclo.isEmpty()) {
                return ciclo;
            }
        }
        camino.pop();
        terminados.add(nodo);
        return List.of();
    }

    /**
     * Heurística simple: una firma de método a 4 u 8 espacios de sangría que abre una llave, hasta la llave de cierre con
     * la misma sangría. No entiende firmas partidas en varias líneas, pero alcanza para frenar los métodos largos.
     */
    static List<String> metodosLargos(String texto, int maximo) {
        Pattern firma = Pattern.compile("^( {4}| {8})(?!\\s*(if|for|while|switch|try|catch|else|do|return|new|throw)\\b)"
                + "[\\w<>\\[\\],.? @]+\\s+(\\w+)\\([^;]*\\)\\s*(throws [\\w, .]+)?\\s*\\{\\s*$");
        List<String> lineas = texto.lines().toList();
        List<String> largos = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            Matcher m = firma.matcher(lineas.get(i));
            if (!m.matches() || lineas.get(i).contains(" class ") || lineas.get(i).contains(" record ")) {
                continue;
            }
            int fin = i + 1;
            while (fin < lineas.size() && !lineas.get(fin).equals(m.group(1) + "}")) {
                fin++;
            }
            int largo = fin - i + 1;
            if (largo > maximo) {
                largos.add(m.group(3) + "() tiene " + largo + " líneas (máximo " + maximo + ")");
            }
        }
        return largos;
    }
}
