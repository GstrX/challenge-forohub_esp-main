package com.forohub.foro_api.service;

import com.forohub.foro_api.controller.TopicoController;
import com.forohub.foro_api.dto.*;
import com.forohub.foro_api.model.Curso;
import com.forohub.foro_api.model.Mensaje;
import com.forohub.foro_api.model.Topico;
import com.forohub.foro_api.repository.TopicoRepository;
import com.forohub.foro_api.repository.MensajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class TopicoService {

    @Autowired
    private TopicoRepository topicoRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    public Topico registrarTopico(DatosRegistroTopico datosRegistroTopico) {
        if (topicoRepository.existsByTituloAndMensajes_contenido(datosRegistroTopico.titulo(), datosRegistroTopico.mensaje())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tópico ya existe.");
        }
        Topico nuevoTopico = new Topico(datosRegistroTopico);
        return topicoRepository.save(nuevoTopico);
    }


    public Page<DatosListadoTopico> listarTopicos(Pageable paginacion) {
        return topicoRepository.findAllActive(paginacion).map(DatosListadoTopico::new);
    }

    public PagedModel<EntityModel<DatosListadoTopico>> convertirAPagedModel(Page<DatosListadoTopico> topicosPage,
                                                                            PagedResourcesAssembler<DatosListadoTopico> pagedResourcesAssembler,
                                                                            Pageable paginacion) {
        return pagedResourcesAssembler.toModel(topicosPage,
                topico -> EntityModel.of(topico,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TopicoController.class)
                                .listadoTopicos(paginacion)).withSelfRel()));
    }

    public Page<DatosListadoTopico> buscarTopicosPorCurso(String nombreCurso, Pageable paginacion) {
        Curso curso;
        try {
            curso = Curso.valueOf(nombreCurso.toUpperCase()); // Convertir el String a Curso enum
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curso inválido");
        }
        return topicoRepository.findByCursoAndStatusNotClosed(curso, paginacion).map(DatosListadoTopico::new);
    }

    public Optional<Topico> buscarTopicoPorId(Long id) {

        return topicoRepository.findById(id);
    }

    @Transactional
    public void actualizarTopico(Long id, DatosActualizarTopico datosActualizarTopico) {
        Topico topico = topicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tópico no encontrado"));

        topico.actualizarTopico(datosActualizarTopico);

        // Guardar los cambios en el repositorio
        topicoRepository.save(topico);
    }

    public DatosListadoMensaje obtenerUltimoMensaje(Long id) {
        Topico topico = topicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tópico no encontrado"));

        List<Mensaje> mensajes = topico.getMensajes();
        if (!mensajes.isEmpty()) {
            Mensaje ultimoMensaje = mensajes.get(mensajes.size() - 1);
            return new DatosListadoMensaje(
                    ultimoMensaje.getId(),
                    ultimoMensaje.getContenido(),
                    ultimoMensaje.getFecha(),
                    ultimoMensaje.getAutor()
            );
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay mensajes en el tópico");
        }
    }

    @Transactional
    public DatosListadoMensaje agregarMensaje(Long id, DatosNuevoMensaje datosNuevoMensaje) {
        Topico topico = topicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tópico no encontrado"));

        Mensaje nuevoMensaje = new Mensaje(datosNuevoMensaje);

        topico.agregarMensaje(nuevoMensaje);

        topicoRepository.save(topico);
        return new DatosListadoMensaje(nuevoMensaje);
    }


    @Transactional
    public void cerrarTopico(Long id) {
        Topico topico = topicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tópico no encontrado"));

        topico.cerrarTopico();

        topicoRepository.save(topico);
    }

    public void eliminarMensaje(Long idTopico, Long idMensaje) {
        Topico topico = topicoRepository.findById(idTopico)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tópico no encontrado"));

        Mensaje mensaje = topico.getMensajes().stream()
                .filter(m -> m.getId().equals(idMensaje))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));

        topico.getMensajes().remove(mensaje);

        topicoRepository.save(topico);

        mensajeRepository.deleteById(idMensaje);
    }
}
