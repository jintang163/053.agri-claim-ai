package com.agri.claim.assess.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.agri.claim.assess.entity.AssessDetail;
import com.agri.claim.assess.entity.AssessMission;
import com.agri.claim.assess.mapper.AssessDetailMapper;
import com.agri.claim.assess.mapper.AssessMissionMapper;
import com.agri.claim.common.config.MinioConfig;
import com.agri.claim.common.constant.Constants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportService {

    private final AssessMissionMapper missionMapper;
    private final AssessDetailMapper detailMapper;
    private final MinioConfig minioConfig;
    private final Font titleFont;
    private final Font headerFont;
    private final Font contentFont;
    private final Font smallFont;

    public ReportService(AssessMissionMapper missionMapper, AssessDetailMapper detailMapper,
                         MinioConfig minioConfig) {
        this.missionMapper = missionMapper;
        this.detailMapper = detailMapper;
        this.minioConfig = minioConfig;
        try {
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            this.titleFont = new Font(bfChinese, 18, Font.BOLD);
            this.headerFont = new Font(bfChinese, 12, Font.BOLD);
            this.contentFont = new Font(bfChinese, 10.5f, Font.NORMAL);
            this.smallFont = new Font(bfChinese, 9f, Font.NORMAL);
        } catch (Exception e) {
            throw new RuntimeException("字体初始化失败", e);
        }
    }

    public byte[] generatePdf(Long missionId) {
        AssessMission mission = missionMapper.selectById(missionId);
        if (mission == null) {
            throw new RuntimeException("定损任务不存在: " + missionId);
        }
        List<AssessDetail> details = detailMapper.selectList(new LambdaQueryWrapper<AssessDetail>()
                .eq(AssessDetail::getMissionId, missionId));
        mission.setDetails(details);
        byte[] pdfBytes = generatePdf(mission);
        String pdfKey = "reports/" + DateUtil.format(DateUtil.date(), "yyyy/MM/dd")
                + "/" + missionId + "_" + IdUtil.fastSimpleUUID().substring(0, 8) + ".pdf";
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pdfBytes)) {
            minioConfig.uploadFile(bais, pdfBytes.length,
                    Constants.MINIO_BUCKET_IMAGE, pdfKey, "application/pdf");
            mission.setReportPdfKey(pdfKey);
            mission.setReportTime(LocalDateTime.now());
            if (mission.getReportNo() == null || mission.getReportNo().isBlank()) {
                mission.setReportNo("RPT" + System.currentTimeMillis());
            }
            missionMapper.updateById(mission);
            log.info("定损报告已上传MinIO | missionId: {} | pdfKey: {}", missionId, pdfKey);
        } catch (Exception e) {
            log.warn("报告上传MinIO失败，已生成PDF字节但未归档", e);
        }
        return pdfBytes;
    }

    public boolean pushToCoreSystem(Long missionId) {
        AssessMission mission = missionMapper.selectById(missionId);
        if (mission == null) {
            log.warn("推送核心系统失败，任务不存在: {}", missionId);
            return false;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("missionNo", mission.getMissionNo());
        payload.put("reportNo", mission.getReportNo());
        payload.put("policyNo", mission.getPolicyNo());
        payload.put("policyHolderName", mission.getPolicyHolderName());
        payload.put("idCardNo", mission.getIdCardNo());
        payload.put("disasterType", mission.getDisasterType());
        payload.put("disasterLevel", mission.getDisasterLevel());
        payload.put("disasterArea", mission.getDisasterArea());
        payload.put("disasterRatio", mission.getDisasterRatio());
        payload.put("finalAmount", mission.getFinalAmount());
        payload.put("reportPdfKey", mission.getReportPdfKey());
        payload.put("pushTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.info("模拟推送定损结果至保险公司核心业务系统 | missionId: {} | 金额: {} | payload: {}",
                missionId, mission.getFinalAmount(), payload);
        return true;
    }

    public byte[] generatePdf(AssessMission mission) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 60, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            addCoverPage(document, mission);
            document.newPage();
            addBasicInfoSection(document, mission);
            addDisasterInfoSection(document, mission);
            addCompensateDetailSection(document, mission);
            addSummarySection(document, mission);
            addSignatureSection(document, mission);

            document.close();
            log.info("定损报告生成成功 | missionId: {} | 页数: {}", mission.getId(),
                    document.getPageNumber());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("定损报告生成失败", e);
            throw new RuntimeException(e);
        }
    }

    private void addCoverPage(Document document, AssessMission mission) throws Exception {
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        Paragraph title = new Paragraph("农业保险定损报告", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(40);
        document.add(title);

        document.add(Chunk.NEWLINE);

        String[] labels = {"报告编号:", "任务编号:", "保单号:", "被保险人:",
                "作物类型:", "灾害类型:", "报告时间:"};
        String[] values = {
                nvl(mission.getReportNo()),
                nvl(mission.getMissionNo()),
                nvl(mission.getPolicyNo()),
                nvl(mission.getPolicyHolderName()),
                nvl(mission.getCropType()),
                disasterTypeText(mission.getDisasterType()),
                mission.getReportTime() != null ?
                        mission.getReportTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : mission.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        };

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(70);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoTable.setSpacingBefore(20);
        infoTable.setSpacingAfter(40);

        for (int i = 0; i < labels.length; i++) {
            infoTable.addCell(createCell(labels[i], headerFont, Element.ALIGN_RIGHT, 30, BaseColor.LIGHT_GRAY));
            infoTable.addCell(createCell(values[i], contentFont, Element.ALIGN_LEFT, 30, BaseColor.WHITE));
        }
        document.add(infoTable);

        document.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("—— 农业保险快速定损系统 ——", headerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addBasicInfoSection(Document document, AssessMission mission) throws Exception {
        document.add(createSectionHeader("一、基本信息"));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2, 1.2f, 2});
        table.setSpacingBefore(8);

        addRow(table, true, "任务名称", nvl(mission.getMissionName()), "定损任务", nvl(mission.getMissionNo()));
        addRow(table, false, "被保险人", nvl(mission.getPolicyHolderName()), "身份证号", maskIdCard(mission.getIdCardNo()));
        addRow(table, true, "联系电话", maskPhone(mission.getPhone()), "保险地址", nvl(mission.getAddress()));
        addRow(table, false, "投保作物", nvl(mission.getCropType()), "投保面积",
                areaText(mission.getInsuredArea()));
        addRow(table, true, "投保金额", amountText(mission.getInsuredAmount()), "查勘员",
                nvl(mission.getSurveyorName()));
        document.add(table);
    }

    private void addDisasterInfoSection(Document document, AssessMission mission) throws Exception {
        document.add(createSectionHeader("二、灾害信息"));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2, 1.2f, 2});
        table.setSpacingBefore(8);

        addRow(table, true, "灾害类型", disasterTypeText(mission.getDisasterType()),
                "受灾等级", disasterLevelText(mission.getDisasterLevel()));
        addRow(table, false, "灾害日期", nvl(mission.getDisasterDate()),
                "受灾地点", nvl(mission.getDisasterLocation()));
        addRow(table, true, "受灾中心经度",
                mission.getDisasterCenterLon() != null ? mission.getDisasterCenterLon().toPlainString() : "-",
                "受灾中心纬度",
                mission.getDisasterCenterLat() != null ? mission.getDisasterCenterLat().toPlainString() : "-");
        addRow(table, false, "灾前影像ID",
                mission.getBeforeImageId() != null ? mission.getBeforeImageId().toString() : "-",
                "灾后影像ID",
                mission.getAfterImageId() != null ? mission.getAfterImageId().toString() : "-");
        addRow(table, true, "总受灾面积", areaText(mission.getDisasterArea()),
                "受灾比例", percentText(mission.getDisasterRatio()));
        document.add(table);
    }

    private void addCompensateDetailSection(Document document, AssessMission mission) throws Exception {
        document.add(createSectionHeader("三、赔付明细"));

        List<AssessDetail> details = mission.getDetails();
        if (details == null || details.isEmpty()) {
            document.add(new Paragraph("暂无明细数据", contentFont));
            return;
        }

        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.6f, 1, 0.9f, 1.2f, 1, 1, 1.1f, 1.2f, 1.3f});
        table.setSpacingBefore(8);

        String[] headers = {"序号", "作物类型", "灾害等级", "地块面积(亩)", "受灾面积(亩)",
                "亩产标准(kg)", "单价(元/kg)", "赔付系数", "赔付金额(元)"};
        for (String h : headers) {
            table.addCell(createCell(h, headerFont, Element.ALIGN_CENTER, 28, BaseColor.LIGHT_GRAY));
        }

        int idx = 1;
        for (AssessDetail d : details) {
            boolean alt = idx % 2 == 0;
            BaseColor bg = alt ? new BaseColor(245, 245, 245) : BaseColor.WHITE;
            table.addCell(createCell(String.valueOf(idx++), smallFont, Element.ALIGN_CENTER, 26, bg));
            table.addCell(createCell(nvl(d.getCropType()), smallFont, Element.ALIGN_CENTER, 26, bg));
            table.addCell(createCell(disasterLevelText(d.getDisasterLevel()), smallFont, Element.ALIGN_CENTER, 26, bg));
            table.addCell(createCell(areaText(d.getPlotArea()), smallFont, Element.ALIGN_RIGHT, 26, bg));
            table.addCell(createCell(areaText(d.getDisasterArea()), smallFont, Element.ALIGN_RIGHT, 26, bg));
            table.addCell(createCell(numberText(d.getUnitYield()), smallFont, Element.ALIGN_RIGHT, 26, bg));
            table.addCell(createCell(numberText(d.getUnitPrice()), smallFont, Element.ALIGN_RIGHT, 26, bg));
            table.addCell(createCell(coeffText(d.getDisasterCoeff(), d.getAdjustCoeff()),
                    smallFont, Element.ALIGN_CENTER, 26, bg));
            table.addCell(createCell(amountText(d.getFinalAmount()), smallFont, Element.ALIGN_RIGHT, 26, bg));
        }

        // 合计行
        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal totalDisaster = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (AssessDetail d : details) {
            if (d.getPlotArea() != null) totalArea = totalArea.add(d.getPlotArea());
            if (d.getDisasterArea() != null) totalDisaster = totalDisaster.add(d.getDisasterArea());
            if (d.getFinalAmount() != null) totalAmount = totalAmount.add(d.getFinalAmount());
        }
        BaseColor sumBg = new BaseColor(220, 235, 250);
        table.addCell(createCell("合计", headerFont, Element.ALIGN_CENTER, 28, sumBg));
        for (int i = 0; i < 2; i++) table.addCell(createCell("-", headerFont, Element.ALIGN_CENTER, 28, sumBg));
        table.addCell(createCell(areaText(totalArea), headerFont, Element.ALIGN_RIGHT, 28, sumBg));
        table.addCell(createCell(areaText(totalDisaster), headerFont, Element.ALIGN_RIGHT, 28, sumBg));
        for (int i = 0; i < 3; i++) table.addCell(createCell("-", headerFont, Element.ALIGN_CENTER, 28, sumBg));
        table.addCell(createCell(amountText(totalAmount), headerFont, Element.ALIGN_RIGHT, 28, sumBg));

        document.add(table);
    }

    private void addSummarySection(Document document, AssessMission mission) throws Exception {
        document.add(createSectionHeader("四、赔付汇总"));

        Paragraph p1 = new Paragraph();
        p1.setFont(contentFont);
        p1.setFirstLineIndent(24);
        p1.setSpacingAfter(8);
        p1.add("经无人机影像AI智能定损分析，本次" + disasterTypeText(mission.getDisasterType())
                + "灾害共造成被保险人 \"" + nvl(mission.getPolicyHolderName())
                + "\" 承包的" + nvl(mission.getCropType())
                + "作物受灾。经核查，投保面积 " + areaText(mission.getInsuredArea())
                + "，实际受灾面积 " + areaText(mission.getDisasterArea())
                + "，受灾比例 " + percentText(mission.getDisasterRatio())
                + "，综合受灾等级为 " + disasterLevelText(mission.getDisasterLevel()) + "。");
        document.add(p1);

        Paragraph p2 = new Paragraph();
        p2.setFont(contentFont);
        p2.setFirstLineIndent(24);
        p2.setSpacingAfter(8);
        p2.add("根据保险条款及定损标准，结合植被指数变化分析与地块级别损失评估，"
                + "本次定损预估赔付金额为：");
        document.add(p2);

        Paragraph amount = new Paragraph();
        amount.setFont(new Font(titleFont));
        amount.setAlignment(Element.ALIGN_CENTER);
        amount.setSpacingBefore(10);
        amount.setSpacingAfter(10);
        amount.add("¥ " + amountText(mission.getFinalAmount()) + " 元");
        document.add(amount);

        Paragraph p3 = new Paragraph();
        p3.setFont(smallFont);
        p3.setAlignment(Element.ALIGN_CENTER);
        p3.setSpacingAfter(15);
        p3.add("（大写：" + toChineseAmount(mission.getFinalAmount()) + "）");
        document.add(p3);
    }

    private void addSignatureSection(Document document, AssessMission mission) throws Exception {
        document.add(createSectionHeader("五、签字确认"));

        PdfPTable sigTable = new PdfPTable(4);
        sigTable.setWidthPercentage(100);
        sigTable.setSpacingBefore(30);
        sigTable.setSpacingAfter(20);

        String[] titles = {"查勘员签字:", "被保险人签字:", "审核人签字:", "保险公司盖章:"};
        for (String t : titles) {
            PdfPCell cell = new PdfPCell(new Phrase(t, headerFont));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setMinimumHeight(110);
            cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            cell.setBorderWidthBottom(1);
            sigTable.addCell(cell);
        }
        document.add(sigTable);

        Paragraph dateLine = new Paragraph("出具日期：    年    月    日", contentFont);
        dateLine.setAlignment(Element.ALIGN_RIGHT);
        dateLine.setSpacingBefore(15);
        document.add(dateLine);

        Paragraph hint = new Paragraph();
        hint.setFont(new Font(smallFont));
        hint.setAlignment(Element.ALIGN_CENTER);
        hint.setSpacingBefore(30);
        hint.add("—— 本报告由AI智能定损系统自动生成，仅供参考，最终赔付以保险公司核准为准 ——");
        document.add(hint);
    }

    private Paragraph createSectionHeader(String text) {
        Paragraph p = new Paragraph(text, headerFont);
        p.setSpacingBefore(18);
        p.setSpacingAfter(4);
        p.setFirstLineIndent(-24);
        return p;
    }

    private void addRow(PdfPTable table, boolean alt, String label1, String value1,
                        String label2, String value2) {
        BaseColor bg = alt ? new BaseColor(245, 245, 245) : BaseColor.WHITE;
        table.addCell(createCell(label1, headerFont, Element.ALIGN_RIGHT, 24,
                new BaseColor(220, 235, 250)));
        table.addCell(createCell(value1, contentFont, Element.ALIGN_LEFT, 24, bg));
        table.addCell(createCell(label2, headerFont, Element.ALIGN_RIGHT, 24,
                new BaseColor(220, 235, 250)));
        table.addCell(createCell(value2, contentFont, Element.ALIGN_LEFT, 24, bg));
    }

    private PdfPCell createCell(String text, Font font, int align, float minH, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        cell.setPaddingTop(4);
        cell.setPaddingBottom(4);
        cell.setMinimumHeight(minH);
        if (bg != null) cell.setBackgroundColor(bg);
        return cell;
    }

    private String nvl(String s) { return s == null || s.isBlank() ? "-" : s; }

    private String areaText(BigDecimal v) {
        return v == null ? "-" : v.setScale(2, RoundingMode.HALF_UP).toPlainString() + " 亩";
    }

    private String amountText(BigDecimal v) {
        return v == null ? "-" : v.setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String numberText(BigDecimal v) {
        return v == null ? "-" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String percentText(BigDecimal v) {
        return v == null ? "-" : v.setScale(2, RoundingMode.HALF_UP).toPlainString() + " %";
    }

    private String coeffText(BigDecimal disaster, BigDecimal adjust) {
        String s = "×" + (disaster == null ? "0.50" : disaster.setScale(2, RoundingMode.HALF_UP).toPlainString());
        if (adjust != null && adjust.compareTo(BigDecimal.ONE) != 0) {
            s += "×" + adjust.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return s;
    }

    private String disasterTypeText(String type) {
        if (type == null) return "-";
        return switch (type) {
            case "FLOOD" -> "淹水灾害";
            case "LODGE" -> "倒伏灾害";
            case "WITHER" -> "枯黄灾害";
            default -> type;
        };
    }

    private String disasterLevelText(String level) {
        if (level == null) return "-";
        return switch (level) {
            case "LIGHT" -> "轻度";
            case "MODERATE" -> "中度";
            case "SEVERE" -> "重度";
            default -> level;
        };
    }

    private String maskPhone(String p) {
        if (p == null || p.length() < 7) return nvl(p);
        return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
    }

    private String maskIdCard(String id) {
        if (id == null || id.length() < 10) return nvl(id);
        return id.substring(0, 4) + "********" + id.substring(id.length() - 4);
    }

    private String toChineseAmount(BigDecimal amount) {
        if (amount == null) return "零元整";
        try {
            String[] digit = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
            String[] unit1 = {"", "拾", "佰", "仟"};
            String[] unit2 = {"", "万", "亿", "兆"};
            String s = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String[] parts = s.split("\\.");
            long num = Long.parseLong(parts[0]);
            if (num == 0) return "零元" + (parts[1].equals("00") ? "整" : centsText(parts[1]));
            StringBuilder sb = new StringBuilder();
            String numStr = String.valueOf(num);
            int len = numStr.length();
            for (int i = 0; i < len; i++) {
                int d = numStr.charAt(i) - '0';
                int pos = len - 1 - i;
                int unitIdx = pos / 4;
                int subIdx = pos % 4;
                if (d != 0) {
                    sb.append(digit[d]).append(unit1[subIdx]);
                } else if (subIdx == 0 && unitIdx < unit2.length) {
                    sb.append(unit2[unitIdx]);
                }
                if (subIdx == 0 && unitIdx > 0 && unitIdx < unit2.length
                        && !sb.toString().endsWith(unit2[unitIdx])) {
                    sb.append(unit2[unitIdx]);
                }
            }
            sb.append("元");
            if (parts[1].equals("00")) sb.append("整");
            else sb.append(centsText(parts[1]));
            return sb.toString().replaceAll("零+", "零").replace("零万", "万")
                    .replace("零亿", "亿").replace("零元", "元");
        } catch (Exception e) {
            return amount + " 元";
        }
    }

    private String centsText(String s) {
        String[] digit = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        StringBuilder sb = new StringBuilder();
        if (s.charAt(0) != '0') sb.append(digit[s.charAt(0) - '0']).append("角");
        if (s.charAt(1) != '0') sb.append(digit[s.charAt(1) - '0']).append("分");
        return sb.toString();
    }
}
