package co.edu.unbosque.trabajofinalback.dto;

/**
 * Datos de un contacto. Se usa para recibir (agregar contacto) y para responder
 * (listar).
 *
 * <p>
 * Para agregar se envía: usuario (nombre de usuario o correo) y alias
 * (opcional). Al listar se devuelve: id (id del usuario contacto), usuario,
 * nombre (alias o usuario) y alias.
 */
public class ContactDTO {
	private Long id;
	private String usuario;
	private String nombre;
	private String alias;

	public ContactDTO() {
	}

	public ContactDTO(Long id, String usuario, String nombre, String alias) {
		this.id = id;
		this.usuario = usuario;
		this.nombre = nombre;
		this.alias = alias;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
}