package co.edu.unbosque.trabajofinalback.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Un chat en la lista del panel admin. */
public class ChatAdminDTO {

	private Long id;
	private String code;
	private List<String> participants;
	private LocalDateTime lastMessageAt;
	private long messageCount;

	public ChatAdminDTO() {
		// TODO Auto-generated constructor stub
	}

	public ChatAdminDTO(Long id, String code, List<String> participants, LocalDateTime lastMessageAt,
			long messageCount) {
		this.id = id;
		this.code = code;
		this.participants = participants;
		this.lastMessageAt = lastMessageAt;
		this.messageCount = messageCount;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public List<String> getParticipants() {
		return participants;
	}

	public void setParticipants(List<String> participants) {
		this.participants = participants;
	}

	public LocalDateTime getLastMessageAt() {
		return lastMessageAt;
	}

	public void setLastMessageAt(LocalDateTime lastMessageAt) {
		this.lastMessageAt = lastMessageAt;
	}

	public long getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(long messageCount) {
		this.messageCount = messageCount;
	}

	@Override
	public String toString() {
		return "ChatAdminDTO [id=" + id + ", code=" + code + ", participants=" + participants + ", lastMessageAt="
				+ lastMessageAt + ", messageCount=" + messageCount + "]";
	}
	
	
}
