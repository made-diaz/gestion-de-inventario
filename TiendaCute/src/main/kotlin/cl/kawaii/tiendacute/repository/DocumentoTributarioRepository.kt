package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.DocumentoTributario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentoTributarioRepository : JpaRepository<DocumentoTributario, Long>
