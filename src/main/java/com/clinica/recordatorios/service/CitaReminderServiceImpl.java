package com.clinica.recordatorios.service;

import com.clinica.recordatorios.channel.ReminderChannel;
import com.clinica.recordatorios.channel.ReminderChannelFactory;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public int enviarRecordatoriosPendientes() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime hasta = ahora.plusHours(24);

        List<Cita> citasPendientes = citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(ahora, hasta);

        for (Cita cita : citasPendientes) {
            ReminderChannel canal = reminderChannelFactory.obtenerCanal(cita.getCanalPreferido());
            String mensaje = "Estimado/a " + cita.getPacienteNombre() + ", le recordamos su cita programada para " + cita.getFechaHoraCita() + ".";
            canal.enviar(cita, mensaje);
            cita.setRecordatorioEnviado(true);
            citaRepository.save(cita);
        }

        return citasPendientes.size();
    }

    @Override
    public void enviarRecordatorioUrgente(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new CitaNoEncontradaException(citaId));

        String mensajeUrgente = "AVISO URGENTE: Estimado/a " + cita.getPacienteNombre() + ", confirmación inmediata requerida para su cita.";
        canalUrgente.enviar(cita, mensajeUrgente);
        cita.setRecordatorioEnviado(true);
        citaRepository.save(cita);
    }
}
