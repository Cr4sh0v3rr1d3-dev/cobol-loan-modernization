      *> Detalle de una cuota individual de la tabla de amortización.
      *> Se serializa una línea JSON por cuota (JSON GENERATE no
      *> soporta tablas OCCURS — verificado, ver CLAUDE.md sección 6),
      *> a continuación de la línea de resumen (WS-LOAN-RESPONSE) en
      *> stdout. Solo se emite cuando RES-COD-ERROR = 0.
       01  WS-CUOTA-DETALLE.
           05  CUOTA-NUMERO           PIC 9(3).
           05  CUOTA-INTERES          PIC 9(9)V99 COMP-3.
           05  CUOTA-AMORTIZACION     PIC 9(9)V99 COMP-3.
