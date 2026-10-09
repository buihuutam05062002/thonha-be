package com.thonha.backend.entity;

import com.thonha.backend.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tin nhắn trong cuộc trò chuyện của một yêu cầu sửa chữa (khách hàng <-> thợ được ghép).
 * Mỗi yêu cầu có đúng một cuộc trò chuyện nên tin nhắn gắn trực tiếp với repair_request.
 */
@Entity
@Table(name = "message", indexes = @Index(name = "idx_message_request", columnList = "request_id, id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @ToString.Exclude
    private RepairRequest request;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @ToString.Exclude
    private User sender;

    // Có thể null nếu tin nhắn chỉ gồm ảnh
    @Column(columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "is_seen", nullable = false)
    @Builder.Default
    private boolean seen = false;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<MessageImage> images = new ArrayList<>();

    public void addImage(String url) {
        images.add(MessageImage.builder().message(this).url(url).build());
    }
}
