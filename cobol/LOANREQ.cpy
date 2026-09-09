      *> Contrato de entrada a SIMLOAN.cbl.
      *> Se puebla parseando la línea de stdin "monto,plazo,tasa"
      *> con UNSTRING + FUNCTION NUMVAL (no JSON PARSE: no está
      *> implementado en GnuCOBOL, ver CLAUDE.md sección 2 y 6).
       01  WS-LOAN-REQUEST.
           05  REQ-MONTO-SOLICITADO   PIC 9(9)V99 COMP-3.
           05  REQ-PLAZO-MESES        PIC 9(3).
           05  REQ-TASA-ANUAL         PIC 9(3)V999 COMP-3.
