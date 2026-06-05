package com.example.hotel.service;

import com.example.hotel.entity.Conversation;
import com.example.hotel.entity.ConversationTurn;
import com.example.hotel.entity.MessageRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JsonlExportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObjectMapper objectMapper;

    public JsonlExportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJsonl(List<Conversation> conversations) {
        StringBuilder builder = new StringBuilder();
        for (Conversation conversation : conversations) {
            try {
                builder.append(objectMapper.writeValueAsString(toFineTuningObject(conversation))).append('\n');
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Could not serialize conversation " + conversation.getId(), ex);
            }
        }
        return builder.toString();
    }

    public String toCsv(List<Conversation> conversations) {
        StringBuilder builder = new StringBuilder("id,title,category,status,created_at,ai_corrected,user_message,assistant_message\n");
        for (Conversation conversation : conversations) {
            builder.append(csv(conversation.getId()))
                    .append(',')
                    .append(csv(conversation.getTitle()))
                    .append(',')
                    .append(csv(conversation.getCategory()))
                    .append(',')
                    .append(csv(conversation.getStatus()))
                    .append(',')
                    .append(csv(conversation.getCreatedAt() == null ? "" : DATE_TIME.format(conversation.getCreatedAt())))
                    .append(',')
                    .append(csv(conversation.isAiCorrected() ? "yes" : "no"))
                    .append(',')
                    .append(csv(firstContent(conversation, MessageRole.USER)))
                    .append(',')
                    .append(csv(firstContent(conversation, MessageRole.ASSISTANT)))
                    .append('\n');
        }
        return builder.toString();
    }

    public byte[] toPdfReport(List<Conversation> conversations, List<String> categoriesIncluded) {
        List<String> lines = new ArrayList<>();
        lines.add("Aurora Hotel Conversation Export Report");
        lines.add("Total conversations: " + conversations.size());
        lines.add("Categories included: " + (categoriesIncluded.isEmpty() ? "None" : String.join(", ", categoriesIncluded)));
        lines.add("");
        for (Conversation conversation : conversations.stream().limit(25).toList()) {
            lines.add("#" + conversation.getId() + " " + conversation.getTitle());
            lines.add("Category: " + conversation.getCategory() + " | Status: " + conversation.getStatus()
                    + " | AI corrected: " + (conversation.isAiCorrected() ? "yes" : "no"));
            lines.add("User: " + firstContent(conversation, MessageRole.USER));
            lines.add("Assistant: " + firstContent(conversation, MessageRole.ASSISTANT));
            lines.add("");
        }
        if (conversations.size() > 25) {
            lines.add("Report preview includes the first 25 conversations. Use CSV or JSONL for the full dataset.");
        }
        return simplePdf(lines);
    }

    private Map<String, Object> toFineTuningObject(Conversation conversation) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are a helpful hotel assistant."));
        for (ConversationTurn turn : conversation.getTurns()) {
            messages.add(Map.of(
                    "role", turn.getRole() == MessageRole.USER ? "user" : "assistant",
                    "content", turn.getContent()));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messages", messages);
        return payload;
    }

    private String firstContent(Conversation conversation, MessageRole role) {
        return conversation.getTurns().stream()
                .filter(turn -> turn.getRole() == role)
                .map(ConversationTurn::getContent)
                .findFirst()
                .orElse("");
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private byte[] simplePdf(List<String> lines) {
        StringBuilder content = new StringBuilder("BT\n/F1 11 Tf\n50 780 Td\n14 TL\n");
        for (String line : lines.stream().limit(52).toList()) {
            content.append('(').append(pdf(line)).append(") Tj\nT*\n");
        }
        content.append("ET\n");

        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + content.toString().getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + content + "endstream"
        );

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.US_ASCII).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xrefOffset = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String pdf(String value) {
        String ascii = value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
        if (ascii.length() > 92) {
            ascii = ascii.substring(0, 89) + "...";
        }
        return ascii.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
