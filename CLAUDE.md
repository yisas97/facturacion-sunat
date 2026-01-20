# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sistema de facturación electrónica para SUNAT (Superintendencia Nacional de Aduanas y de Administración Tributaria) de Perú. Aplicación Spring Boot para gestión de comprobantes electrónicos.

## Build Commands

```bash
# Compilar el proyecto
./mvnw compile

# Ejecutar la aplicación
./mvnw spring-boot:run

# Ejecutar todos los tests
./mvnw test

# Ejecutar un test específico
./mvnw test -Dtest=FacturacionSunatApplicationTests

# Empaquetar la aplicación
./mvnw package

# Limpiar y compilar
./mvnw clean compile
```

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.9**
- **Lombok** - para reducir boilerplate code
- **Maven** - gestión de dependencias y build

## Project Structure

```
src/main/java/pe/com/movimientos/facturacionsunat/
└── FacturacionSunatApplication.java    # Entry point

src/main/resources/
└── application.properties              # Configuración

src/test/java/pe/com/movimientos/facturacionsunat/
└── FacturacionSunatApplicationTests.java
```

## Development Notes

- Usar anotaciones de Lombok (@Data, @Builder, @Getter, @Setter, etc.) para entidades y DTOs
- DevTools está habilitado para hot reload durante desarrollo
