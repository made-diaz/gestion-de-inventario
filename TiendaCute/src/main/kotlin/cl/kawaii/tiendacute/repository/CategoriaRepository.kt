package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Categoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository


@Repository
interface CategoriaRepository : JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria>
