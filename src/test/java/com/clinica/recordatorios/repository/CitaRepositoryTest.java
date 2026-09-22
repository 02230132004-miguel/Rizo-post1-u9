package com.clinica.recordatorios.repository;

import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class CitaRepositoryTest {

    @Autowired
    private CitaRepository citaRepository;

    @Test
    @DisplayName("Debe retornar exclusivamente las citas dentro de la ventana que aún no han sido enviadas")
    void findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse_soloRetornaCitasPendientesEnVentana() {
        // Arrange: Definir ventana temporal
        LocalDateTime ventanaInicio = LocalDateTime.of(2026, 8, 10, 9, 0);
        LocalDateTime ventanaFin = LocalDateTime.of(2026, 8, 11, 9, 0);

        // 1. Cita dentro de la ventana y pendiente (debe ser retornada)
        Cita citaPendienteEnVentana = new Cita(
                "Ana Gomez",
                "ana@example.com",
                LocalDateTime.of(2026, 8, 10, 14, 0),
                CanalNotificacion.EMAIL,
                false
        );

        // 2. Cita dentro de la ventana pero ya enviada (no debe ser retornada)
        Cita citaYaEnviadaEnVentana = new Cita(
                "Pedro Pascal",
                "pedro@example.com",
                LocalDateTime.of(2026, 8, 10, 16, 0),
                CanalNotificacion.EMAIL,
                true
        );

        // 3. Cita fuera de la ventana y pendiente (no debe ser retornada)
        Cita citaPendienteFueraVentana = new Cita(
                "Lucia Mendez",
                "+573009998877",
                LocalDateTime.of(2026, 8, 15, 10, 0),
                CanalNotificacion.SMS,
                false
        );

        citaRepository.saveAll(List.of(citaPendienteEnVentana, citaYaEnviadaEnVentana, citaPendienteFueraVentana));

        // Act
        List<Cita> resultado = citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(ventanaInicio, ventanaFin);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        Cita citaRecuperada = resultado.get(0);
        assertEquals("Ana Gomez", citaRecuperada.getPacienteNombre());
        assertFalse(citaRecuperada.isRecordatorioEnviado());
        assertEquals(LocalDateTime.of(2026, 8, 10, 14, 0), citaRecuperada.getFechaHoraCita());
    }
}
