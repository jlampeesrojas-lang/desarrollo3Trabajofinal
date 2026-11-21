package co.edu.unbosque.trabajofinalback.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Un chat en la lista del panel admin. */
public class ChatAdminDTO {

	private Long id;
	private String code;
	private List<String> participants = new ArrayList<>();
	private LocalDateTime lastMessageAt;
	private long messageCount;

	/** Constructor vacío. */
	public ChatAdminDTO() {
		// TODO Auto-generated constructor stub
	}

	/** Crea un ChatAdminDTO con sus datos. */
	public ChatAdminDTO(Long id, String code, List<String> participants, LocalDateTime lastMessageAt,
			long messageCount) {
		this.id = id;
		this.code = code;
		this.participants = participants == null ? new ArrayList<>() : new ArrayList<>(participants);
		this.lastMessageAt = lastMessageAt;
		this.messageCount = messageCount;
	}

	/** Devuelve id. */
	public Long getId() {
		return id;
	}

	/** Define id. */
	public void setId(Long id) {
		this.id = id;
	}

	/** Devuelve code. */
	public String getCode() {
		return code;
	}

	/** Define code. */
	public void setCode(String code) {
		this.code = code;
	}

	/** Devuelve participants. */
	public List<String> getParticipants() {
		return new ArrayList<>(participants);
	}

	/** Define participants. */
	public void setParticipants(List<String> participants) {
		this.participants = participants == null ? new ArrayList<>() : new ArrayList<>(participants);
	}

	/** Devuelve last message at. */
	public LocalDateTime getLastMessageAt() {
		return lastMessageAt;
	}

	/** Define last message at. */
	public void setLastMessageAt(LocalDateTime lastMessageAt) {
		this.lastMessageAt = lastMessageAt;
	}

	/** Devuelve message count. */
	public long getMessageCount() {
		return messageCount;
	}

	/** Define message count. */
	public void setMessageCount(long messageCount) {
		this.messageCount = messageCount;
	}

	/** Devuelve el objeto como texto. */
	@Override
	public String toString() {
		return "ChatAdminDTO [id=" + id + ", code=" + code + ", participants=" + participants + ", lastMessageAt="
				+ lastMessageAt + ", messageCount=" + messageCount + "]";
	}

}