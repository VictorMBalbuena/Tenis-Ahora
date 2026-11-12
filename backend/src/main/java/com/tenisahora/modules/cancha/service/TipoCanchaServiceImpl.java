package com.tenisahora.modules.cancha.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.cancha.dto.TipoCanchaRequest;
import com.tenisahora.modules.cancha.dto.TipoCanchaResponse;
import com.tenisahora.modules.cancha.entity.TipoCancha;
import com.tenisahora.modules.cancha.mapper.TipoCanchaMapper;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.repository.TipoCanchaRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipoCanchaServiceImpl implements TipoCanchaService {

    private final TipoCanchaRepository tipoCanchaRepository;
    private final CanchaRepository canchaRepository;
    private final TipoCanchaMapper tipoCanchaMapper;
    private final PdfService pdfService;

    @Override
    @Transactional
    public TipoCanchaResponse registrar(TipoCanchaRequest request, Long actorId) {
        asegurarNombreLibre(request.nombre(), null);
        TipoCancha guardado = tipoCanchaRepository.save(tipoCanchaMapper.aEntidad(request));
        log.info("Tipo de cancha {} ('{}') registrado por la persona {}", guardado.getId(), guardado.getNombre(), actorId);
        return tipoCanchaMapper.aRespuesta(guardado, 0);
    }

    @Override
    @Transactional
    public TipoCanchaResponse modificar(Long id, TipoCanchaRequest request, Long actorId) {
        TipoCancha tipo = buscarActivo(id);
        asegurarNombreLibre(request.nombre(), id);
        tipoCanchaMapper.aplicar(request, tipo);
        log.info("Tipo de cancha {} modificado por la persona {}", id, actorId);
        int canchasAsociadas = (int) canchaRepository.countByTipoCanchaIdAndActivaTrue(id);
        return tipoCanchaMapper.aRespuesta(tipo, canchasAsociadas);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoCanchaResponse consultar(Long id) {
        TipoCancha tipo = buscarActivo(id);
        int canchasAsociadas = (int) canchaRepository.countByTipoCanchaIdAndActivaTrue(id);
        return tipoCanchaMapper.aRespuesta(tipo, canchasAsociadas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoCanchaResponse> listar() {
        return tipoCanchaRepository.findAllByActivoTrueOrderByNombreAsc().stream()
                .map(tipo -> {
                    int canchas = (int) canchaRepository.countByTipoCanchaIdAndActivaTrue(tipo.getId());
                    return tipoCanchaMapper.aRespuesta(tipo, canchas);
                })
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(Long id, Long actorId) {
        TipoCancha tipo = buscarActivo(id);
        long canchasAsociadas = canchaRepository.countByTipoCanchaIdAndActivaTrue(id);
        if (canchasAsociadas > 0) {
            throw new ConflictoException(CodigoError.TIPO_CANCHA_EN_USO,
                    "No se puede dar de baja el tipo de cancha porque hay " + canchasAsociadas + " cancha(s) activa(s) asociada(s)");
        }
        tipo.setActivo(false);
        log.info("Tipo de cancha {} dado de baja por la persona {}", id, actorId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReportePdf() {
        List<TipoCancha> tipos = tipoCanchaRepository.findAllByActivoTrueOrderByNombreAsc();
        List<PdfService.CampoFicha> campos = new ArrayList<>();

        for (TipoCancha t : tipos) {
            long canchasCount = canchaRepository.countByTipoCanchaIdAndActivaTrue(t.getId());
            campos.add(new PdfService.CampoFicha("Tipo de cancha", t.getNombre()));
            campos.add(new PdfService.CampoFicha("Superficie", t.getSuperficie()));
            campos.add(new PdfService.CampoFicha("Capacidad", t.getCapacidad() + " jugadores"));
            campos.add(new PdfService.CampoFicha("Canchas operativas", String.valueOf(canchasCount)));
            if (t.getDescripcion() != null && !t.getDescripcion().isBlank()) {
                campos.add(new PdfService.CampoFicha("Descripción", t.getDescripcion()));
            }
        }

        return pdfService.generarFicha("Reporte de Tipos de Canchas y Características", campos);
    }

    private TipoCancha buscarActivo(Long id) {
        return tipoCanchaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.TIPO_CANCHA_NO_ENCONTRADO, "Tipo de cancha no encontrado"));
    }

    private void asegurarNombreLibre(String nombre, Long idPropio) {
        boolean duplicado = idPropio == null
                ? tipoCanchaRepository.existsByNombreIgnoreCase(nombre)
                : tipoCanchaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, idPropio);
        if (duplicado) {
            throw new ConflictoException(CodigoError.TIPO_CANCHA_NOMBRE_DUPLICADO, "Ya existe un tipo de cancha con ese nombre");
        }
    }
}

