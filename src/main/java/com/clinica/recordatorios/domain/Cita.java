package com.clinica.recordatorios.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pacienteNombre;

    private String pacienteContacto;

    private LocalDateTime fechaHoraCita;

    @Enumerated(EnumType.STRING)
    private CanalNotificacion canalPreferido;

    private boolean recordatorioEnviado;

    public Cita() {
    }

    public Cita(Long id, String pacienteNombre, String pacienteContacto, LocalDateTime fechaHoraCita, CanalNotificacion canalPreferido, boolean recordatorioEnviado) {
        this.id = id;
        this.pacienteNombre = pacienteNombre;
        this.pacienteContacto = pacienteContacto;
        this.fechaHoraCita = fechaHoraCita;
        this.canalPreferido = canalPreferido;
        this.recordatorioEnviado = recordatorioEnviado;
    }

    public Cita(String pacienteNombre, String pacienteContacto, LocalDateTime fechaHoraCita, CanalNotificacion canalPreferido, boolean recordatorioEnviado) {
        this(null, pacienteNombre, pacienteContacto, fechaHoraCita, canalPreferido, recordatorioEnviado);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPacienteNombre() {
        return pacienteNombre;
    }

    public void setPacienteNombre(String pacienteNombre) {
        this.pacienteNombre = pacienteNombre;
    }

    public String getPacienteContacto() {
        return pacienteContacto;
    }

    public void setPacienteContacto(String pacienteContacto) {
        this.pacienteContacto = pacienteContacto;
    }

    public LocalDateTime getFechaHoraCita() {
        return fechaHoraCita;
    }

    public void setFechaHoraCita(LocalDateTime fechaHoraCita) {
        this.fechaHoraCita = fechaHoraCita;
    }

    public CanalNotificacion getCanalPreferido() {
        return canalPreferido;
    }

    public void setCanalPreferido(CanalNotificacion canalPreferido) {
        this.canalPreferido = canalPreferido;
    }

    public boolean isRecordatorioEnviado() {
        return recordatorioEnviado;
    }

    public void setRecordatorioEnviado(boolean recordatorioEnviado) {
        this.recordatorioEnviado = recordatorioEnviado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cita cita = (Cita) o;
        return Objects.equals(id, cita.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Cita{" +
                "id=" + id +
                ", pacienteNombre='" + pacienteNombre + '\'' +
                ", pacienteContacto='" + pacienteContacto + '\'' +
                ", fechaHoraCita=" + fechaHoraCita +
                ", canalPreferido=" + canalPreferido +
                ", recordatorioEnviado=" + recordatorioEnviado +
                '}';
    }
}
