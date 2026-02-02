package com.infragest.infra_notifications_service.repository;

import com.infragest.infra_notifications_service.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para gestionar operaciones CRUD en la entidad EmailTemplate.
 * Proporciona métodos adicionales para buscar templates específicos.
 *
 * @author
 * @since 2026-01-27
 */
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    /**
     * Busca un template de correo por su nombre único.
     *
     * @param name Nombre único del template utilizado como identificador.
     * @return EmailTemplate si se encuentra, o vacío en caso contrario.
     */
    EmailTemplate findByName(String name);

}
