-- =====================================================
-- SCRIPT PARA CREAR BASE DE DATOS FACTURACION SUNAT
-- SQL Server 2019
-- =====================================================

-- 1. Crear la base de datos
CREATE DATABASE facturacion_sunat;
GO

USE facturacion_sunat;
GO

-- =====================================================
-- NOTA: Las siguientes tablas se crean automaticamente
-- por JPA con spring.jpa.hibernate.ddl-auto=update
-- Este script es solo de referencia o para crearlas manualmente
-- =====================================================

-- 2. Tabla de Emisores (empresas que emiten comprobantes)
CREATE TABLE emisores (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ruc VARCHAR(11) NOT NULL UNIQUE,
    razon_social VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    direccion VARCHAR(300),
    ubigeo VARCHAR(6),
    departamento VARCHAR(50),
    provincia VARCHAR(50),
    distrito VARCHAR(50),
    usuario_sol VARCHAR(20) NOT NULL,
    clave_sol VARCHAR(50) NOT NULL,
    certificado_pfx VARBINARY(MAX),  -- Certificado digital en bytes
    clave_certificado VARCHAR(100),
    ambiente VARCHAR(15) DEFAULT 'BETA',  -- BETA o PRODUCCION
    series_factura VARCHAR(100),  -- F001,F002,F003
    series_boleta VARCHAR(100),   -- B001,B002,B003
    activo BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2
);
GO

-- 3. Tabla de Comprobantes emitidos
CREATE TABLE comprobantes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    emisor_id BIGINT NOT NULL,
    tipo_comprobante VARCHAR(2) NOT NULL,  -- 01=Factura, 03=Boleta, 07=NC, 08=ND
    serie VARCHAR(4) NOT NULL,
    correlativo INT NOT NULL,
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE,
    moneda VARCHAR(3) DEFAULT 'PEN',
    tipo_operacion VARCHAR(4) DEFAULT '0101',
    forma_pago VARCHAR(20) DEFAULT 'Contado',

    -- Datos del cliente
    cliente_tipo_documento VARCHAR(1) NOT NULL,  -- 6=RUC, 1=DNI
    cliente_numero_documento VARCHAR(15) NOT NULL,
    cliente_razon_social VARCHAR(200) NOT NULL,
    cliente_direccion VARCHAR(300),

    -- Totales
    total_gravadas DECIMAL(12,2) DEFAULT 0,
    total_exoneradas DECIMAL(12,2) DEFAULT 0,
    total_inafectas DECIMAL(12,2) DEFAULT 0,
    total_igv DECIMAL(12,2) DEFAULT 0,
    total_venta DECIMAL(12,2) DEFAULT 0,

    -- XML y respuesta SUNAT
    xml_firmado VARCHAR(MAX),
    cdr_xml VARCHAR(MAX),
    cdr_codigo VARCHAR(20),
    cdr_mensaje VARCHAR(500),

    -- Estado
    estado VARCHAR(20) DEFAULT 'PENDIENTE',  -- PENDIENTE, ACEPTADO, RECHAZADO, ANULADO
    observaciones VARCHAR(500),

    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2,

    CONSTRAINT FK_comprobante_emisor FOREIGN KEY (emisor_id) REFERENCES emisores(id),
    CONSTRAINT UQ_comprobante UNIQUE (emisor_id, tipo_comprobante, serie, correlativo)
);
GO

-- 4. Tabla de Items de comprobante
CREATE TABLE comprobante_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    comprobante_id BIGINT NOT NULL,
    numero_item INT NOT NULL,
    codigo VARCHAR(50),
    descripcion VARCHAR(500) NOT NULL,
    unidad_medida VARCHAR(5) DEFAULT 'NIU',  -- NIU=Unidad, ZZ=Servicio
    cantidad DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(12,4) NOT NULL,  -- Sin IGV
    precio_con_igv DECIMAL(12,4),
    tipo_afectacion_igv VARCHAR(2) DEFAULT '10',  -- 10=Gravado, 20=Exonerado, 30=Inafecto
    porcentaje_igv DECIMAL(5,2) DEFAULT 18.00,
    subtotal DECIMAL(12,2),
    igv DECIMAL(12,2),
    total DECIMAL(12,2),

    CONSTRAINT FK_item_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobantes(id) ON DELETE CASCADE
);
GO

-- 5. Indices para mejorar rendimiento
CREATE INDEX IX_emisores_ruc ON emisores(ruc);
CREATE INDEX IX_comprobantes_emisor ON comprobantes(emisor_id);
CREATE INDEX IX_comprobantes_fecha ON comprobantes(fecha_emision);
CREATE INDEX IX_comprobantes_estado ON comprobantes(estado);
CREATE INDEX IX_comprobantes_cliente ON comprobantes(cliente_numero_documento);
GO

-- =====================================================
-- DATOS DE PRUEBA (OPCIONAL)
-- =====================================================

-- Insertar emisor de prueba (sin certificado)
INSERT INTO emisores (ruc, razon_social, nombre_comercial, direccion, ubigeo,
    departamento, provincia, distrito, usuario_sol, clave_sol, ambiente,
    series_factura, series_boleta, activo)
VALUES (
    '20123456789',
    'EMPRESA DE PRUEBA SAC',
    'EMPRESA PRUEBA',
    'AV. PRUEBA 123',
    '150101',
    'LIMA',
    'LIMA',
    'LIMA',
    'MODDATOS',    -- Usuario SOL de prueba SUNAT
    'MODDATOS',    -- Clave SOL de prueba SUNAT
    'BETA',
    'F001,F002',
    'B001,B002',
    1
);
GO

PRINT 'Base de datos creada exitosamente';
PRINT 'NOTA: Debe subir el certificado .pfx via API: POST /api/emisores/{id}/certificado';
GO
