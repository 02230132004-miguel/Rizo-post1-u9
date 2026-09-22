package com.clinica.recordatorios;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import com.clinica.recordatorios.service.CitaReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
class CitaReminderServiceIntegrationTest {

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock clock() {
            return Clock.fixed(Instant.parse("2026-08-10T08:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @MockBean
    private NotificadorSmsGateway notificadorSmsGateway;

    @Autowired
    private CitaReminderService citaReminderService;

    @Autowired
    private CitaRepository citaRepository;

    @BeforeEach
    void setUp() {
        citaRepository.deleteAll();
    }

    @Test
    @DisplayName("Debe ejecutar el flujo completo con contexto de Spring, notificando por SMS y persistiendo el estado en H2")
    void enviarRecordatoriosPendientes_contextoCompletoDeSpring_persisteYNotificaCitasEnVentana() {
        // Arrange: Insertar una cita con canal SMS dentro de la ventana de 24h (reloj fijo en 2026-08-10T08:00:00Z)
        LocalDateTime horaCita = LocalDateTime.of(2026, 8, 10, 14, 30);
        Cita citaSms = new Cita(
                "Carlos Valderrama",
                "+573001112233",
                horaCita,
                CanalNotificacion.SMS,
                false
        );
        Cita citaGuardada = citaRepository.save(citaSms);

        // Act: Ejecutar el servicio sobre el contexto real de Spring Boot
        int procesadas = citaReminderService.enviarRecordatoriosPendientes();

        // Assert: Validar procesamiento, interacción con el Gateway SMS mockeado y persistencia en H2
        assertEquals(1, procesadas);
        verify(notificadorSmsGateway).enviarSms(eq("+573001112233"), anyString());

        Optional<Cita> citaActualizada = citaRepository.findById(citaGuardada.getId());
        assertTrue(citaActualizada.isPresent());
        assertTrue(citaActualizada.get().isRecordatorioEnviado());
    }
}
