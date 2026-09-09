-- Datos reales de tasas de préstamos personales, bancos argentinos.
-- Snapshot al 2026-09-02. Fuente: iproup.com (ver docs/investigacion-cobol-modernizacion-rest.md, sección 8).
-- No inventar ni "redondear" estos valores: si hace falta actualizarlos,
-- traer un snapshot nuevo con su propia fuente y fecha_vigencia.

INSERT INTO tasas_bancarias (banco, producto, tna, cft, fecha_vigencia, fuente) VALUES
    ('Banco Galicia',     'Éminent',                          39.000,  58.900, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Macro',       'Jubilados y Selecta',              55.000,  91.100, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Nación',      'Jubilados con descuento',          56.000,  93.300, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Galicia',     'Personal Plus',                    57.000,  95.800, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Macro',       'Plan Sueldo',                       61.000, 104.700, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Nación',      'Empleados públicos con haberes',   64.000, 111.800, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Ciudad',      'Plan Sueldo',                       70.000, 126.700, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Santander',   'Personal',                          79.000, 150.900, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Patagonia',   'Personal Online',                   98.000, 209.800, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('Banco Hipotecario', 'Destino Libre',                    110.400, 255.200, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026'),
    ('BBVA',              'Oferta exclusiva para clientes',   129.000, 320.100, '2026-09-02', 'https://www.iproup.com/finanzas/71138-prestamos-personales-cuanto-cuesta-pedir-banco-cobra-menos-tasa-total-septiembre-2026');
