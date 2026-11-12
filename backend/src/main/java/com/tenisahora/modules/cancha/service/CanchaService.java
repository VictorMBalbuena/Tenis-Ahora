package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.CanchaRequest;
import com.tenisahora.modules.cancha.dto.CanchaResponse;
import com.tenisahora.modules.cancha.dto.CanchaUpdateRequest;
import com.tenisahora.modules.cancha.dto.EstadoCancha;
import com.tenisahora.shared.dto.PageResponse;

public interface CanchaService {

    /** CU06 */
    CanchaResponse registrar(CanchaRequest request, Long actorId);

    /** CU08 */
    CanchaResponse consultar(Long id);

    /** CU08: filtra por superficie, estado y/o parte del identificador; sin filtros lista todas. */
    PageResponse<CanchaResponse> buscar(String superficie, String identificador, EstadoCancha estado, int page, int size);

    default PageResponse<CanchaResponse> buscar(String superficie, String superficieTexto, String identificador, EstadoCancha estado, int page, int size) {
        String sup = (superficie != null && !superficie.isBlank()) ? superficie : superficieTexto;
        return buscar(sup, identificador, estado, page, size);
    }

    /** CU07 */
    CanchaResponse modificar(Long id, CanchaUpdateRequest request, Long actorId);
}
