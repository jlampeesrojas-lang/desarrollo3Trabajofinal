package co.edu.unbosque.trabajofinalback.repository;

import co.edu.unbosque.trabajofinalback.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de archivos multimedia. */
public interface MediaRepository extends JpaRepository<Media, Long> {}