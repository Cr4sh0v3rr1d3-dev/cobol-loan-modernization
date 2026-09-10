      *> Motor de cálculo de cuota de préstamo, sistema francés.
      *> Sin acceso a datos: la tasa llega ya resuelta desde Java
      *> (ver CLAUDE.md sección 2). Entrada por stdin como línea
      *> "monto,plazo,tasa"; salida por stdout como JSON.
       IDENTIFICATION DIVISION.
       PROGRAM-ID. SIMLOAN.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-INPUT-LINE              PIC X(100).
       01  WS-MONTO-STR               PIC X(20).
       01  WS-PLAZO-STR               PIC X(20).
       01  WS-TASA-STR                PIC X(20).

       COPY LOANREQ.
       COPY LOANRES.
       COPY LOANCUOTA.

       01  WS-TASA-MENSUAL            PIC 9V9(6) COMP-3.
       01  WS-FACTOR                  PIC 9V9(6) COMP-3.
       01  WS-FACTOR-N                PIC 9V9(6) COMP-3.
       01  WS-DENOMINADOR             PIC 9V9(6) COMP-3.
       01  WS-SALDO-PENDIENTE         PIC 9(9)V99 COMP-3.
       01  I                          PIC 9(3).

       01  WS-JSON-OUT                PIC X(500).

       PROCEDURE DIVISION.
       MAIN-PROCEDURE.
           ACCEPT WS-INPUT-LINE FROM CONSOLE

           UNSTRING WS-INPUT-LINE DELIMITED BY ","
               INTO WS-MONTO-STR WS-PLAZO-STR WS-TASA-STR
           END-UNSTRING

           COMPUTE REQ-MONTO-SOLICITADO = FUNCTION NUMVAL(WS-MONTO-STR)
           COMPUTE REQ-PLAZO-MESES = FUNCTION NUMVAL(WS-PLAZO-STR)
           COMPUTE REQ-TASA-ANUAL = FUNCTION NUMVAL(WS-TASA-STR)

           MOVE 0 TO RES-COD-ERROR
           MOVE SPACES TO RES-MENSAJE-ERROR

           EVALUATE TRUE
               WHEN REQ-MONTO-SOLICITADO <= 0
                   MOVE 1 TO RES-COD-ERROR
                   MOVE "Monto solicitado debe ser mayor a cero"
                       TO RES-MENSAJE-ERROR
               WHEN REQ-PLAZO-MESES <= 0 OR REQ-PLAZO-MESES > 360
                   MOVE 2 TO RES-COD-ERROR
                   MOVE "Plazo debe estar entre 1 y 360 meses"
                       TO RES-MENSAJE-ERROR
               WHEN REQ-TASA-ANUAL <= 0
                   MOVE 3 TO RES-COD-ERROR
                   MOVE "Tasa anual debe ser mayor a cero"
                       TO RES-MENSAJE-ERROR
           END-EVALUATE

           IF RES-COD-ERROR = 0
               PERFORM CALCULAR-CUOTA-FRANCES
           END-IF

           JSON GENERATE WS-JSON-OUT FROM WS-LOAN-RESPONSE
               NAME OF WS-LOAN-RESPONSE IS OMITTED
           DISPLAY FUNCTION TRIM(WS-JSON-OUT)

           IF RES-COD-ERROR = 0
               PERFORM GENERAR-TABLA-AMORTIZACION
           END-IF

           STOP RUN.

       GENERAR-TABLA-AMORTIZACION.
           MOVE REQ-MONTO-SOLICITADO TO WS-SALDO-PENDIENTE

           PERFORM VARYING I FROM 1 BY 1 UNTIL I > REQ-PLAZO-MESES
               COMPUTE CUOTA-INTERES ROUNDED =
                   WS-SALDO-PENDIENTE * WS-TASA-MENSUAL
               COMPUTE CUOTA-AMORTIZACION ROUNDED =
                   RES-CUOTA-MENSUAL - CUOTA-INTERES
               COMPUTE WS-SALDO-PENDIENTE =
                   WS-SALDO-PENDIENTE - CUOTA-AMORTIZACION
               MOVE I TO CUOTA-NUMERO

               MOVE SPACES TO WS-JSON-OUT
               JSON GENERATE WS-JSON-OUT FROM WS-CUOTA-DETALLE
                   NAME OF WS-CUOTA-DETALLE IS OMITTED
               DISPLAY FUNCTION TRIM(WS-JSON-OUT)
           END-PERFORM

           EXIT PARAGRAPH.

       CALCULAR-CUOTA-FRANCES.
           COMPUTE WS-TASA-MENSUAL ROUNDED =
               REQ-TASA-ANUAL / 12 / 100
               ON SIZE ERROR
                   MOVE 9 TO RES-COD-ERROR
                   MOVE "Error de cálculo en tasa mensual"
                       TO RES-MENSAJE-ERROR
           END-COMPUTE

           IF RES-COD-ERROR NOT = 0
               EXIT PARAGRAPH
           END-IF

           IF WS-TASA-MENSUAL = 0
               COMPUTE RES-CUOTA-MENSUAL ROUNDED =
                   REQ-MONTO-SOLICITADO / REQ-PLAZO-MESES
                   ON SIZE ERROR
                       MOVE 9 TO RES-COD-ERROR
                       MOVE "Error de cálculo en cuota"
                           TO RES-MENSAJE-ERROR
               END-COMPUTE
           ELSE
               COMPUTE WS-FACTOR = 1 + WS-TASA-MENSUAL
               COMPUTE WS-FACTOR-N =
                   WS-FACTOR ** (- REQ-PLAZO-MESES)
               COMPUTE WS-DENOMINADOR = 1 - WS-FACTOR-N
               COMPUTE RES-CUOTA-MENSUAL ROUNDED =
                   REQ-MONTO-SOLICITADO * WS-TASA-MENSUAL
                       / WS-DENOMINADOR
                   ON SIZE ERROR
                       MOVE 9 TO RES-COD-ERROR
                       MOVE "Error de cálculo en cuota"
                           TO RES-MENSAJE-ERROR
               END-COMPUTE
           END-IF

           IF RES-COD-ERROR NOT = 0
               EXIT PARAGRAPH
           END-IF

           COMPUTE RES-TOTAL-A-PAGAR ROUNDED =
               RES-CUOTA-MENSUAL * REQ-PLAZO-MESES
               ON SIZE ERROR
                   MOVE 9 TO RES-COD-ERROR
                   MOVE "Error de cálculo en total a pagar"
                       TO RES-MENSAJE-ERROR
           END-COMPUTE

           IF RES-COD-ERROR = 0
               COMPUTE RES-TOTAL-INTERESES ROUNDED =
                   RES-TOTAL-A-PAGAR - REQ-MONTO-SOLICITADO
           END-IF

           EXIT PARAGRAPH.
