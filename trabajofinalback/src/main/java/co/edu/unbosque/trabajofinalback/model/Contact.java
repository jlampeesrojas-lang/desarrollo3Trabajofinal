package co.edu.unbosque.trabajofinalback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;


@Entity
@Table(
    name = "contacts",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_contacts_owner_contact",
            columnNames = {"owner_id", "contact_user_id"}))
public class Contact {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contact_user_id", nullable = false)
  private User contactUser;

  
  @Column(length = 50)
  private String alias;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Contact() {}

  public Contact(User owner, User contactUser, String alias) {
    this.owner = owner;
    this.contactUser = contactUser;
    this.alias = alias;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public Long getId() { return id; }
  public User getOwner() { return owner; }
  public User getContactUser() { return contactUser; }
  public String getAlias() { return alias; }
  public void setAlias(String alias) { this.alias = alias; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}