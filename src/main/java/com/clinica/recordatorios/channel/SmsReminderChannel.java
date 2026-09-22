package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.Cita;
import org.springframework.stereotype.Component;

@Component
public class SmsReminderChannel implements ReminderChannel {

    private final NotificadorSmsGateway notificadorSmsGateway;

    public SmsReminderChannel(NotificadorSmsGateway notificadorSmsGateway) {
        this.notificadorSmsGateway = notificadorSmsGateway;
    }

    @Override
    public void enviar(Cita cita, String mensaje) {
        notificadorSmsGateway.enviarSms(cita.getPacienteContacto(), mensaje);
    }
}
