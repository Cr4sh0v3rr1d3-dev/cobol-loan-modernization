      *> Contrato de salida de SIMLOAN.cbl.
      *> Se serializa con JSON GENERATE ... NAME OF WS-LOAN-RESPONSE
      *> IS OMITTED, que da un objeto JSON plano por stdout.
      *> RES-COD-ERROR = 00 es éxito; distinto de 00 es un error de
      *> negocio real (monto/plazo/tasa fuera de rango), no defensivo.
       01  WS-LOAN-RESPONSE.
           05  RES-CUOTA-MENSUAL      PIC 9(9)V99 COMP-3.
           05  RES-TOTAL-A-PAGAR      PIC 9(9)V99 COMP-3.
           05  RES-TOTAL-INTERESES    PIC 9(9)V99 COMP-3.
           05  RES-COD-ERROR          PIC 9(2).
           05  RES-MENSAJE-ERROR      PIC X(80).
