package com.tenisahora.modules.cobro.service;

public interface AcreditacionDePagos {

    /**
     * CU78: procesa un pago de Mercado Pago identificado por {@code idExterno}. No confía en lo que diga quien
     * avisó (el webhook, o la conciliación): siempre vuelve a consultar la orden a la pasarela antes de acreditar
     * (A08). Si el cobro ya no está esperando acreditación (ya pagado o en otro estado final), no hace nada: un
     * aviso repetido no registra el pago dos veces (CU78 A2).
     */
    void acreditar(String idExterno);
}
