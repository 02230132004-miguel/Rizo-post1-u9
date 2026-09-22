package com.clinica.recordatorios.service;

import com.clinica.recordatorios.channel.ReminderChannel;
import com.clinica.recordatorios.channel.ReminderChannelFactory;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaReminderServiceImplTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ReminderChannelFactory reminderChannelFactory;

    @Mock
    private ReminderChannel reminderChannelMock;

    @Mock
    private ReminderChannel canalUrgenteMock;

    private final Clock clockFijo = Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"));

    private CitaReminderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CitaReminderServiceImpl(
                citaRepository,
                reminderChannelFactory,
                canalUrgenteMock,
                clockFijo
        );
    }

    @Test
    @DisplayName("1. Debe usar el canal preferido de la cita y persistir el estado enviado cuando la cita está en ventana")
    void enviarRecordatoriosPendientes_citaEnVentana_usaElCanalPreferido() {
        // Arrange
        LocalDateTime fechaCita = LocalDateTime.of(2026, 8, 10, 15, 0);
        Cita cita = new Cita(1L, "Juan Perez", "juan@example.com", fechaCita, CanalNotificacion.EMAIL, false);

        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(cita));
        when(reminderChannelFactory.obtenerCanal(CanalNotificacion.EMAIL))
                .thenReturn(reminderChannelMock);

        // Act
        int totalProcesados = service.enviarRecordatoriosPendientes();

        // Assert
        assertEquals(1, totalProcesados);
        verify(reminderChannelFactory).obtenerCanal(CanalNotificacion.EMAIL);
        verify(reminderChannelMock).enviar(eq(cita), anyString());
        assertTrue(cita.isRecordatorioEnviado());
        verify(citaRepository).save(cita);
    }

    @Test
    @DisplayName("2. Debe capturar y verificar los límites exactos de la ventana temporal según el Clock fijo")
    void enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos() {
        // Arrange
        LocalDateTime esperadoInicio = LocalDateTime.of(2026, 8, 10, 9, 0, 0);
        LocalDateTime esperadoFin = LocalDateTime.of(2026, 8, 11, 9, 0, 0);

        ArgumentCaptor<LocalDateTime> captorInicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> captorFin = ArgumentCaptor.forClass(LocalDateTime.class);

        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(captorInicio.capture(), captorFin.capture()))
                .thenReturn(Collections.emptyList());

        // Act
        service.enviarRecordatoriosPendientes();

        // Assert
        assertEquals(esperadoInicio, captorInicio.getValue());
        assertEquals(esperadoFin, captorFin.getValue());
    }

    @Test
    @DisplayName("3. No debe consultar la Factory ni persistir nada cuando no hay citas pendientes")
    void enviarRecordatoriosPendientes_sinCitasPendientes_noConsultaLaFactory() {
        // Arrange
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        int totalProcesados = service.enviarRecordatoriosPendientes();

        // Assert
        assertEquals(0, totalProcesados);
        verifyNoInteractions(reminderChannelFactory);
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("4. Debe enviar el recordatorio usando el canal urgente inyectado y actualizar el estado de la cita existente")
    void enviarRecordatorioUrgente_citaExistente_usaCanalUrgenteInyectado() {
        // Arrange
        Long citaId = 100L;
        LocalDateTime fechaCita = LocalDateTime.of(2026, 8, 10, 11, 30);
        Cita cita = new Cita(citaId, "Maria Lopez", "+573001234567", fechaCita, CanalNotificacion.SMS, false);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));

        // Act
        service.enviarRecordatorioUrgente(citaId);

        // Assert
        verify(canalUrgenteMock).enviar(eq(cita), anyString());
        assertTrue(cita.isRecordatorioEnviado());
        verify(citaRepository).save(cita);
    }

    @Test
    @DisplayName("5. No debe marcar como enviado ni persistir si el gateway de canal urgente falla lanzando excepción")
    void enviarRecordatorioUrgente_gatewayFalla_noMarcaComoEnviado() {
        // Arrange
        Long citaId = 200L;
        LocalDateTime fechaCita = LocalDateTime.of(2026, 8, 10, 12, 0);
        Cita cita = new Cita(citaId, "Carlos Ruiz", "+573119876543", fechaCita, CanalNotificacion.SMS, false);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        doThrow(new RuntimeException("Error en gateway SMS")).when(canalUrgenteMock).enviar(any(Cita.class), anyString());

        // Act & Assert
        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> service.enviarRecordatorioUrgente(citaId));
        assertEquals("Error en gateway SMS", excepcion.getMessage());
        assertFalse(cita.isRecordatorioEnviado());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("6. Debe lanzar CitaNoEncontradaException y tener cero interacciones con el canal si la cita no existe")
    void enviarRecordatorioUrgente_citaInexistente_lanzaCitaNoEncontradaException() {
        // Arrange
        Long citaIdInexistente = 999L;
        when(citaRepository.findById(citaIdInexistente)).thenReturn(Optional.empty());

        // Act & Assert
        CitaNoEncontradaException exception = assertThrows(
                CitaNoEncontradaException.class,
                () -> service.enviarRecordatorioUrgente(citaIdInexistente)
        );

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("999"));
        verifyNoInteractions(canalUrgenteMock);
        verify(citaRepository, never()).save(any(Cita.class));
    }
}
