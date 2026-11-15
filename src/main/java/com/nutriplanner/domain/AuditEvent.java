package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;
import com.nutriplanner.storage.EntityMapper;

import java.time.Instant;

/** A security-relevant event: logins, password changes and administrative actions. */
public final class AuditEvent {

    public static final EntityMapper<AuditEvent> MAPPER =
            EntityMapper.of(AuditEvent::getId, AuditEvent::setId, AuditEvent::toDoc, AuditEvent::fromDoc);

    private long id;
    private Instant at;
    private AuditType type;
    private long actorId;
    private String actorName;
    private String detail;
    private String ip;

    public AuditEvent() {
    }

    public AuditEvent(Instant at, AuditType type, long actorId, String actorName, String detail, String ip) {
        this.at = at;
        this.type = type;
        this.actorId = actorId;
        this.actorName = actorName;
        this.detail = detail;
        this.ip = ip;
    }

    public Doc toDoc() {
        return new Doc().put("at", at).put("type", type).put("actorId", actorId).put("actorName", actorName)
                .put("detail", detail).put("ip", ip);
    }

    public static AuditEvent fromDoc(long id, Doc d) {
        AuditEvent e = new AuditEvent();
        e.id = id;
        e.at = d.getInstant("at");
        e.type = d.getEnum("type", AuditType.class, AuditType.LOGIN_SUCCESS);
        e.actorId = d.getLong("actorId", 0);
        e.actorName = d.getString("actorName", "");
        e.detail = d.getString("detail", "");
        e.ip = d.getString("ip", "");
        return e;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Instant getAt() { return at; }
    public AuditType getType() { return type; }
    public long getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public String getDetail() { return detail; }
    public String getIp() { return ip; }
}
