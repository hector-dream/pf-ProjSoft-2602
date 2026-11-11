package br.insper.provaFinal.artigo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtigoRepository extends JpaRepository<Artigo, Long> {
    // SOFT DELETE (2/3): descomente os dois métodos para consultar apenas registros ativos.
    // java.util.Optional<Avaliacao> findByIdAndExcluidaFalse(Long id);
    // java.util.List<Avaliacao> findAllByExcluidaFalse(org.springframework.data.domain.Sort sort);
}
