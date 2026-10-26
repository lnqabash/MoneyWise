package app.moneymate.scam;

import app.moneymate.common.ApiException;
import app.moneymate.scam.dto.ScamDtos.Advice;
import app.moneymate.scam.dto.ScamDtos.RedFlag;
import app.moneymate.scam.dto.ScamDtos.ScanBody;
import app.moneymate.scam.dto.ScamDtos.ScanResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ScamService {

    private final ScamDetectionEngine engine;
    private final ScamReportRepository reports;
    private final ObjectMapper objectMapper;

    public ScamService(ScamDetectionEngine engine, ScamReportRepository reports, ObjectMapper objectMapper) {
        this.engine = engine;
        this.reports = reports;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ScanResponse scan(UUID userId, ScanBody body) {
        var verdict = engine.classify(body.content());

        ScamReport report = ScamReport.create(
                userId, body.channel(), hash(body.content()),
                verdict.riskLevel(), verdict.riskScore(),
                toJson(verdict.redFlags()), ScamDetectionEngine.MODEL_VERSION);
        reports.save(report);

        return new ScanResponse(
                report.getReportId().toString(), verdict.riskLevel(), verdict.riskScore(),
                verdict.redFlags(), adviceFor(verdict.riskLevel()), ScamDetectionEngine.MODEL_VERSION);
    }

    @Transactional
    public app.moneymate.scam.dto.ScamDtos.ActionResponse recordAction(
            UUID userId, UUID reportId, String action) {
        ScamReport report = reports.findByReportIdAndUserId(reportId, userId)
                .orElseThrow(() -> ApiException.notFound("Report not found"));
        report.setUserAction(action);
        reports.save(report);
        return new app.moneymate.scam.dto.ScamDtos.ActionResponse("recorded");
    }

    private Advice adviceFor(String level) {
        return switch (level) {
            case "high" -> new Advice(
                    "Do not click or reply. This is almost certainly a scam.",
                    List.of("block_sender", "report_to_telco", "warn_contacts"));
            case "medium" -> new Advice(
                    "Be careful. Verify the sender through an official channel before doing anything.",
                    List.of("verify_sender", "do_not_share_details"));
            default -> new Advice(
                    "This looks okay, but always double-check who is contacting you.",
                    List.of("stay_alert"));
        };
    }

    private String toJson(List<RedFlag> flags) {
        try {
            return objectMapper.writeValueAsString(flags);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /** Salted hash so raw scam content is never persisted (privacy by design). */
    private String hash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(("moneymate-salt:" + content).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return "unhashed";
        }
    }
}
