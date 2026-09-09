-- Esquema de persistencia para tasas bancarias reales.
-- COBOL no accede a esta tabla: Java resuelve la tasa acá y la manda
-- como input ya resuelto al proceso COBOL (ver CLAUDE.md, sección 2 y 4).

CREATE TABLE tasas_bancarias (
    id              BIGSERIAL PRIMARY KEY,
    banco           TEXT NOT NULL,
    producto        TEXT NOT NULL,
    tna             NUMERIC(6,3) NOT NULL,  -- tasa nominal anual, en %
    cft             NUMERIC(6,3) NOT NULL,  -- costo financiero total / TEA, en %
    fecha_vigencia  DATE NOT NULL,          -- snapshot al que corresponde el dato
    fuente          TEXT NOT NULL,          -- URL de origen, nunca vacío
    UNIQUE (banco, producto, fecha_vigencia)
);

CREATE INDEX idx_tasas_bancarias_banco ON tasas_bancarias (banco);
