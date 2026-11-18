package com.commerceplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sitemap_job_state")
public class SitemapJobState {
    @Id
    private Long id = 1L;

    @Column(name = "site_url", nullable = false, length = 300)
    private String siteUrl = "";

    @Lob
    @Column(name = "sitemap_xml")
    private String sitemapXml;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_run_succeeded")
    private Boolean lastRunSucceeded;

    @Column(name = "url_count", nullable = false)
    private int urlCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

     public SitemapJobState() {}

    public Long getId() { return id; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getSitemapXml() { return sitemapXml; }
    public void setSitemapXml(String sitemapXml) { this.sitemapXml = sitemapXml; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public Instant getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(Instant lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public Boolean getLastRunSucceeded() { return lastRunSucceeded; }
    public void setLastRunSucceeded(Boolean lastRunSucceeded) { this.lastRunSucceeded = lastRunSucceeded; }
    public int getUrlCount() { return urlCount; }
    public void setUrlCount(int urlCount) { this.urlCount = urlCount; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
