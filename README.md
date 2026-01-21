# Proyecto de Facturación Electrónica SUNAT

Este proyecto es un servicio de facturación electrónica para Perú que se integra con los servicios web de la SUNAT. Permite generar, firmar y enviar comprobantes de pago electrónicos (facturas y boletas).

## Características

-   Gestión de múltiples empresas emisoras.
-   Generación de XML para facturas y boletas de venta según el estándar UBL 2.1.
-   Firma digital de los comprobantes con certificado digital (.pfx).
-   Envío de comprobantes a los servicios web de la SUNAT (SOAP).
-   Consulta de estado de envío de comprobantes mediante Ticket.
-   Almacenamiento de información en base de datos SQL Server.

## Tecnologías Utilizadas

-   **Lenguaje:** Java 21
-   **Framework:** Spring Boot 3.2.0
-   **Base de Datos:** Microsoft SQL Server
-   **Acceso a Datos:** Spring Data JPA
-   **Comunicación SOAP:** Apache CXF
-   **Dependencias:** Lombok, JAXB

## Requisitos Previos

-   JDK 21 o superior.
-   Apache Maven 3.6 o superior.
-   Microsoft SQL Server.
-   Un certificado digital (.pfx) para la firma de comprobantes.

## Configuración

1.  **Clonar el repositorio:**
    ```bash
    git clone <URL_DEL_REPOSITORIO>
    cd facturacion-sunat
    ```

2.  **Base de Datos:**
    -   Ejecute el script `scripts/crear_bd.sql` en su instancia de SQL Server para crear la base de datos y las tablas necesarias.
    -   Actualice el archivo `src/main/resources/application.properties` con sus credenciales de base de datos:
        ```properties
        spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=facturacion_sunat;encrypt=true;trustServerCertificate=true;
        spring.datasource.username=<TU_USUARIO>
        spring.datasource.password=<TU_CONTRASEÑA>
        spring.jpa.hibernate.ddl-auto=update
        ```

3.  **Certificado Digital:**
    -   El emisor necesita un certificado digital. Este se sube a través de la API, pero puede configurar uno por defecto si lo desea.

## Ejecución

1.  **Compilar el proyecto:**
    ```bash
    mvn clean install
    ```
    Este comando también generará las clases del cliente SOAP a partir del WSDL de la SUNAT.

2.  **Ejecutar la aplicación:**
    ```bash
    mvn spring-boot:run
    ```
    El servicio estará disponible en `http://localhost:8080`.

## API Endpoints

A continuación se describen los endpoints principales de la API.

### Emisores (`/api/emisores`)

-   `GET /`: Lista todos los emisores.
-   `GET /{id}`: Obtiene un emisor por su ID.
-   `GET /ruc/{ruc}`: Obtiene un emisor por su RUC.
-   `POST /`: Crea un nuevo emisor.
-   `PUT /{id}`: Actualiza un emisor existente.
-   `POST /{id}/certificado`: Sube el archivo de certificado digital (.pfx) y su clave para un emisor.
-   `DELETE /{id}`: Desactiva un emisor.

### Facturación (`/api/sunat`)

-   `POST /factura/emitir`: Endpoint principal. Recibe un JSON con los datos del comprobante, lo genera, firma y envía a la SUNAT.
-   `POST /factura/preview`: Devuelve una vista previa del XML del comprobante sin firmar.
-   `POST /factura/firmar`: Devuelve el XML del comprobante firmado, pero no lo envía a la SUNAT.
-   `GET /correlativo`: Obtiene el siguiente número de correlativo para un tipo de comprobante y serie.
-   `GET /comprobantes?emisorId={id}`: Lista los comprobantes emitidos por un emisor.
-   `GET /ticket/{ticket}?emisorId={id}`: Consulta el estado de un envío a la SUNAT a través del número de ticket.
-   `GET /health`: Verifica el estado del servicio.
