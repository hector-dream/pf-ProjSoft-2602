package br.insper.provaFinal.avaliacao;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ArtigoService {
    private final ArtigoRepository repository;

    public ArtigoService(ArtigoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Artigo cadastrar(CriarArtigoRequest request, String emailAvaliador) {
        return repository.save(new Artigo(emailAvaliador, request));
    }

    @Transactional(readOnly = true)
    public Artigo buscarPorId(Long id) {
        // SOFT DELETE (3/3): troque apenas repository.findById(id) na linha abaixo por
        // repository.findByIdAndExcluidaFalse(id). Mantenha o orElseThrow para retornar 404.
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada."));
    }

    @Transactional
    public Artigo atualizarStatus(Long id, AtualizarStatusRequest request) {
        Artigo avaliacao = buscarPorId(id);
        avaliacao.alterarStatus(request.status());
        return repository.save(avaliacao);
    }

    @Transactional
    public void excluir(Long id) {
        // HARD DELETE (ativo): remova/comente esta linha para ativar a alternativa abaixo.
        repository.delete(buscarPorId(id));

        // SOFT DELETE: descomente as três linhas abaixo após os passos na entidade e repository.
        // Artigo avaliacao = buscarPorId(id);
        // avaliacao.marcarComoExcluida();
        // repository.save(avaliacao);
    }

    @Transactional(readOnly = true)
    public List<Artigo> listar() {
        // SOFT DELETE: comente o return ativo e descomente o return abaixo.
        // return repository.findAllByExcluidaFalse(Sort.by(Sort.Direction.DESC, "dataArtigo", "id"));
        return repository.findAll(Sort.by(Sort.Direction.DESC, "dataArtigo", "id"));
    }
}
