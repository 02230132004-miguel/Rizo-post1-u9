# Sistema de Recordatorio de Citas Médicas (`recordatorios-citas-service`)

[![CI — Pruebas y Cobertura](https://github.com/02230132004-miguel/Rizo-post1-u9/actions/workflows/ci.yml/badge.svg)](https://github.com/02230132004-miguel/Rizo-post1-u9/actions/workflows/ci.yml)

**Estudiante:** Miguel Angel Rizo Arias  
**Repositorio:** `02230132004-miguel/Rizo-post1-u9`  
**Unidad 9:** *Probar el Diseño: JUnit 5, Mockito, Test Doubles e Integración Continua*  
**Stack:** Java 21, Spring Boot 3.3.3, Spring Data JPA, H2 Database (in-memory), JUnit 5, Mockito, GitHub Actions.

---

## 📋 Descripción del Proyecto

Microservicio desarrollado bajo principios de **Clean Architecture** y el **Principio de Inversión de Dependencias (DIP)** para el procesamiento y notificación de recordatorios de citas médicas. El sistema evalúa citas dentro de una ventana temporal determinista de 24 horas y despacha las notificaciones a través de los canales preferidos por los pacientes (`EMAIL` o `SMS`), soportando además el envío prioritario de recordatorios urgentes.

---

## 📐 Decisiones de Diseño

### 1. El Caso de Refactor DIP (Dependency Inversion Principle)

#### 🔴 Problema Previo (Alto Acoplamiento y Código No Testeable)
Originalmente, el servicio instanciaba directamente sus colaboradores dentro de los métodos mediante `new` y consultaba el reloj del sistema mediante `LocalDateTime.now()`:
```java
// Anti-patrón: Alto acoplamiento e imposible de testear unitariamente
public void enviarRecordatorioUrgente(Long citaId) {
    Cita cita = citaRepository.findById(citaId).orElseThrow();
    TwilioNotificadorSmsGateway gateway = new TwilioNotificadorSmsGateway(); // ❌ Acoplamiento rígido
    SmsReminderChannel canal = new SmsReminderChannel(gateway);              // ❌ Instanciación directa
    canal.enviar(cita, "URGENTE...");
}
```
**Consecuencias:**
* Imposibilidad de sustituir `SmsReminderChannel` o `TwilioNotificadorSmsGateway` por un *Test Double* (Mock).
* Las pruebas unitarias dependían de pasarelas reales o fallaban por falta de configuración de red.
* El uso de `LocalDateTime.now()` producía pruebas no deterministas sujetas al momento exacto de ejecución.

#### 🟢 Solución Aplicada (Inversión de Dependencias y Composition Root)
1. Se inyecta la abstracción `ReminderChannel` calificada con `@Qualifier("canalUrgente")` en el constructor de `CitaReminderServiceImpl`.
2. Se inyecta la abstracción `java.time.Clock` para desacoplar el tiempo del sistema operativo.
3. La configuración y ensamblaje de objetos se delega al contenedor de inversión de control en [CanalUrgenteConfig.java](src/main/java/com/clinica/recordatorios/config/CanalUrgenteConfig.java):

```java
@Service
public class CitaReminderServiceImpl implements CitaReminderService {
    private final CitaRepository citaRepository;
    private final ReminderChannelFactory reminderChannelFactory;
    private final ReminderChannel canalUrgente;
    private final Clock clock;

    public CitaReminderServiceImpl(
            CitaRepository citaRepository,
            ReminderChannelFactory reminderChannelFactory,
            @Qualifier("canalUrgente") ReminderChannel canalUrgente,
            Clock clock) {
        this.citaRepository = citaRepository;
        this.reminderChannelFactory = reminderChannelFactory;
        this.canalUrgente = canalUrgente;
        this.clock = clock;
    }
    // Cero uso de "new" dentro de los métodos de negocio
}
```

---

### 2. Mapeo Patrón de Diseño → Estrategia de Prueba

| Patrón / Componente | Propósito en el Diseño | Estrategia de Prueba / Test Double | Clase de Prueba |
| :--- | :--- | :--- | :--- |
| **Strategy Pattern** (`ReminderChannel`) | Define familias de algoritmos de notificación intercambiables (`EMAIL`, `SMS`). | **Mock**: Se simula `ReminderChannel` para verificar que el servicio invoque `enviar()` sin ejecutar I/O real. | `CitaReminderServiceImplTest` |
| **Factory Pattern** (`ReminderChannelFactory`) | Desacopla la selección del canal concreto en tiempo de ejecución según el enum `CanalNotificacion`. | **Mock**: Se programa `when(factory.obtenerCanal(...)).thenReturn(mockChannel)` y se valida `verifyNoInteractions` en ausencia de citas. | `CitaReminderServiceImplTest` |
| **Gateway Pattern** (`NotificadorSmsGateway`) | Aísla la integración HTTP/REST de proveedores externos como Twilio. | **MockBean**: Se inyecta `@MockBean NotificadorSmsGateway` en la prueba de integración para validar la invocación sin consumir saldo ni requerir red. | `CitaReminderServiceIntegrationTest` |
| **Temporal Decoupling** (`java.time.Clock`) | Elimina el no-determinismo temporal en la consulta de citas de las próximas 24 horas. | **Fake**: Se utiliza `Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"))` y `ArgumentCaptor` para corroborar límites exactos. | `CitaReminderServiceImplTest`, `CitaReminderServiceIntegrationTest` |
| **Composition Root** (`CanalUrgenteConfig`, `ClockConfig`) | Centraliza la creación y configuración de dependencias de Spring. | **Spring Boot Test**: Se valida el ensamblado completo del grafo de dependencias en `@SpringBootTest`. | `CitaReminderServiceIntegrationTest` |

---

## 📂 Estructura del Repositorio

```
Rizo-post1-u9/
├── .github/
│   └── workflows/
│       └── ci.yml                             # Pipeline de CI/CD automatizado
├── pom.xml                                    # Descriptor de proyecto y dependencias Maven
├── README.md                                  # Documentación técnica del proyecto
└── src/
    ├── main/
    │   ├── java/com/clinica/recordatorios/
    │   │   ├── RecordatoriosApplication.java  # Punto de entrada Spring Boot
    │   │   ├── channel/
    │   │   │   ├── EmailReminderChannel.java  # Canal Email (SMTP log)
    │   │   │   ├── ReminderChannel.java       # Strategy Interface
    │   │   │   ├── ReminderChannelFactory.java
    │   │   │   ├── ReminderChannelFactoryImpl.java
    │   │   │   ├── SmsReminderChannel.java    # Canal SMS
    │   │   │   └── gateway/
    │   │   │       ├── NotificadorSmsGateway.java
    │   │   │       └── TwilioNotificadorSmsGateway.java
    │   │   ├── config/
    │   │   │   ├── CanalUrgenteConfig.java    # Composition Root para Canal Urgente
    │   │   │   └── ClockConfig.java           # Bean de Clock del sistema
    │   │   ├── domain/
    │   │   │   ├── CanalNotificacion.java     # Enum (EMAIL, SMS)
    │   │   │   └── Cita.java                  # Entidad JPA
    │   │   ├── repository/
    │   │   │   └── CitaRepository.java        # Repositorio Spring Data JPA
    │   │   └── service/
    │   │       ├── CitaNoEncontradaException.java
    │   │       ├── CitaReminderService.java
    │   │       └── CitaReminderServiceImpl.java
    │   └── resources/
    │       └── application.properties         # Configuración H2 y JPA
    └── test/
        └── java/com/clinica/recordatorios/
            ├── CitaReminderServiceIntegrationTest.java # Integración Spring Boot Test
            ├── repository/
            │   └── CitaRepositoryTest.java             # Persistencia DataJpaTest
            └── service/
                └── CitaReminderServiceImplTest.java    # Suite Unitaria JUnit 5 + Mockito
```

---

## 🧪 Evidencia de Pruebas Automatizadas

Todas las pruebas siguen estrictamente la convención de nombrado:  
`metodoBajoPrueba_condicion_resultado`

### Resumen de Ejecución de Pruebas

| Capa / Tipo de Prueba | Clase de Prueba | Método de Prueba | Estado |
| :--- | :--- | :--- | :---: |
| **Unitaria (Mockito)** | `CitaReminderServiceImplTest` | `enviarRecordatoriosPendientes_citaEnVentana_usaElCanalPreferido` | ✅ PASSED |
| **Unitaria (Mockito)** | `CitaReminderServiceImplTest` | `enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos` | ✅ PASSED |
| **Unitaria (Mockito)** | `CitaReminderServiceImplTest` | `enviarRecordatoriosPendientes_sinCitasPendientes_noConsultaLaFactory` | ✅ PASSED |
| **Unitaria (Mockito)** | `CitaReminderServiceImplTest` | `enviarRecordatorioUrgente_citaExistente_usaCanalUrgenteInyectado` | ✅ PASSED |
| **Unitaria (Mockito)** | `CitaReminderServiceImplTest` | `enviarRecordatorioUrgente_gatewayFalla_noMarcaComoEnviado` | ✅ PASSED |
| **Unitaria (Mockito)** | `CitaReminderServiceImplTest` | `enviarRecordatorioUrgente_citaInexistente_lanzaCitaNoEncontradaException` | ✅ PASSED |
| **Persistencia (H2)** | `CitaRepositoryTest` | `findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse_soloRetornaCitasPendientesEnVentana` | ✅ PASSED |
| **Integración Contexto** | `CitaReminderServiceIntegrationTest` | `enviarRecordatoriosPendientes_contextoCompletoDeSpring_persisteYNotificaCitasEnVentana` | ✅ PASSED |

### Reporte de Consola (`mvn clean test`)

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.clinica.recordatorios.service.CitaReminderServiceImplTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.812 s -- in com.clinica.recordatorios.service.CitaReminderServiceImplTest
[INFO] Running com.clinica.recordatorios.repository.CitaRepositoryTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.450 s -- in com.clinica.recordatorios.repository.CitaRepositoryTest
[INFO] Running com.clinica.recordatorios.CitaReminderServiceIntegrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.180 s -- in com.clinica.recordatorios.CitaReminderServiceIntegrationTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 🚀 Pipeline de Integración Continua (GitHub Actions)

El archivo [.github/workflows/ci.yml](.github/workflows/ci.yml) compila y ejecuta las pruebas de forma determinista en cada evento de integración:

```yaml
name: CI — Pruebas y Cobertura

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout del repositorio
        uses: actions/checkout@v4

      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: maven

      - name: Compilar y ejecutar todas las pruebas
        run: mvn -B verify --no-transfer-progress
```

---

## ⚙️ Instrucciones de Ejecución Local

```bash
# 1. Compilar y ejecutar toda la suite de pruebas automatizadas
mvn clean test

# 2. Ejecutar ciclo completo de verificación y empaquetado JAR
mvn clean verify

# 3. Iniciar el servicio localmente
mvn spring-boot:run
```