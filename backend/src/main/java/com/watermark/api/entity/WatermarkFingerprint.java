package com.watermark.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores a perceptual hash (visual fingerprint) for each page
 * of a downloaded file, linked to the user who downloaded it.
 *
 * Used for screenshot matching: when admin uploads a screenshot,
 * compute its hash and search this table for a match.
 */
@Entity
@Table(name = "watermark_fingerprints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatermarkFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productTitle;

    /** Page number within the PDF (0 for images) */
    @Column(nullable = false)
    private int pageNumber;

    /** Perceptual hash (dHash) - 16-char hex string */
    @Column(nullable = false, length = 16)
    private String pageHash;

    @Column(nullable = false)
    private Instant downloadedAt;
}
