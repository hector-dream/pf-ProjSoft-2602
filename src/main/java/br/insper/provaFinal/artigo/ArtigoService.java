package br.insper.provaFinal.artigo;

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
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Artigo não encontrado."));
    }

    @Transactional
    public Artigo atualizarStatus(Long id, AtualizarStatusRequest request) {
        Artigo avaliacao = buscarPorId(id);
        avaliacao.alterarStatus(request.status());
        return repository.save(avaliacao);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarPorId(id));

    }

    @Transactional(readOnly = true)
    public List<Artigo> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "dataAvaliacao", "id"));
    }
}
