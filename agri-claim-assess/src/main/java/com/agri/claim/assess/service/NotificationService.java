package com.agri.claim.assess.service;

import com.agri.claim.assess.websocket.AssessWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AssessWebSocketHandler wsHandler;

    public void notifyAssessProgress(String userId, Long missionId, String stage,
                                     int progress, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ASSESS_PROGRESS");
        payload.put("missionId", missionId);
        payload.put("stage", stage);
        payload.put("progress", progress);
        payload.put("message", message);
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        boolean sent = wsHandler.sendToUser(userId, payload);
        log.info("推送定损进度 | userId: {} | missionId: {} | stage: {} | progress: {}% | sent: {}",
                userId, missionId, stage, progress, sent);
    }

    public void notifyAiProgress(String userId, Long taskId, String step,
                                 int progress, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "AI_PROGRESS");
        payload.put("taskId", taskId);
        payload.put("step", step);
        payload.put("progress", progress);
        payload.put("message", message);
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        boolean sent = wsHandler.sendToUser(userId, payload);
        log.info("推送AI处理进度 | userId: {} | taskId: {} | step: {} | progress: {}% | sent: {}",
                userId, taskId, step, progress, sent);
    }

    public void notifyAuditResult(String userId, Long missionId, boolean approved,
                                  String auditor, String comment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "AUDIT_RESULT");
        payload.put("missionId", missionId);
        payload.put("approved", approved);
        payload.put("auditor", auditor);
        payload.put("comment", comment);
        payload.put("title", approved ? "定损审核通过" : "定损审核驳回");
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        boolean sent = wsHandler.sendToUser(userId, payload);
        log.info("推送审核结果 | userId: {} | missionId: {} | approved: {} | sent: {}",
                userId, missionId, approved, sent);
    }

    public void notifyReportReady(String userId, Long missionId, String reportNo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "REPORT_READY");
        payload.put("missionId", missionId);
        payload.put("reportNo", reportNo);
        payload.put("title", "定损报告已生成");
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        boolean sent = wsHandler.sendToUser(userId, payload);
        log.info("推送报告就绪通知 | userId: {} | missionId: {} | reportNo: {} | sent: {}",
                userId, missionId, reportNo, sent);
    }

    public void notifyDashboard(Map<String, Object> stats) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "DASHBOARD_REFRESH");
        payload.put("stats", stats);
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        wsHandler.broadcast(payload);
        log.debug("广播大屏刷新事件 | 在线: {}", wsHandler.getOnlineCount());
    }

    public int getOnlineUserCount() {
        return wsHandler.getOnlineCount();
    }
}
