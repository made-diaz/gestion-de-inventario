package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Empresa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmpresaRepository : JpaRepository <Empresa, Long>