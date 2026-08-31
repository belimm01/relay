package dev.relay.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.relay.event.Event;
import org.springframework.stereotype.Component;

@Component
public class FilterEvaluator {

    private static final String[] OPERATORS = {">=", "<=", "==", "!=", ">", "<"};
    private static final String CONTAINS = " contains ";

    private final ObjectMapper mapper;

    public FilterEvaluator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public boolean matches(String filterExpr, Event event) {
        if (filterExpr == null || filterExpr.isBlank()) {
            return true;
        }
        JsonNode context = buildContext(event);
        for (String clause : filterExpr.split("&&")) {
            if (!evaluateClause(clause.trim(), context)) {
                return false;
            }
        }
        return true;
    }

    private JsonNode buildContext(Event event) {
        ObjectNode root = mapper.createObjectNode();
        root.set("headers", mapper.valueToTree(event.getHeaders()));
        root.set("body", parseBody(event.getBody()));
        return root;
    }

    private JsonNode parseBody(String body) {
        if (body == null || body.isBlank()) {
            return mapper.nullNode();
        }
        try {
            return mapper.readTree(body);
        } catch (Exception ex) {
            return mapper.getNodeFactory().textNode(body);
        }
    }

    private boolean evaluateClause(String clause, JsonNode context) {
        int containsAt = clause.indexOf(CONTAINS);
        if (containsAt >= 0) {
            JsonNode value = resolve(context, clause.substring(0, containsAt).trim());
            String needle = unquote(clause.substring(containsAt + CONTAINS.length()).trim());
            return !value.isMissingNode() && value.asText().contains(needle);
        }

        for (String op : OPERATORS) {
            int at = clause.indexOf(op);
            if (at > 0) {
                JsonNode value = resolve(context, clause.substring(0, at).trim());
                String literal = clause.substring(at + op.length()).trim();
                return compare(value, op, literal);
            }
        }
        return false;
    }

    private JsonNode resolve(JsonNode context, String path) {
        JsonNode current = context;
        for (String segment : path.split("\\.")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return MissingNode.getInstance();
            }
            current = current.path(segment);
        }
        return current;
    }

    private boolean compare(JsonNode value, String op, String rawLiteral) {
        if (value.isMissingNode()) {
            return op.equals("!=");
        }
        String literal = unquote(rawLiteral);
        Double leftNumber = value.isNumber() ? Double.valueOf(value.asDouble()) : parseNumber(value.asText());
        Double rightNumber = parseNumber(literal);
        boolean numeric = leftNumber != null && rightNumber != null;

        return switch (op) {
            case "==" -> numeric ? leftNumber.equals(rightNumber) : value.asText().equals(literal);
            case "!=" -> numeric ? !leftNumber.equals(rightNumber) : !value.asText().equals(literal);
            case ">" -> numeric && leftNumber > rightNumber;
            case "<" -> numeric && leftNumber < rightNumber;
            case ">=" -> numeric && leftNumber >= rightNumber;
            case "<=" -> numeric && leftNumber <= rightNumber;
            default -> false;
        };
    }

    private static Double parseNumber(String text) {
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String unquote(String literal) {
        if (literal.length() >= 2
                && ((literal.startsWith("\"") && literal.endsWith("\""))
                || (literal.startsWith("'") && literal.endsWith("'")))) {
            return literal.substring(1, literal.length() - 1);
        }
        return literal;
    }
}
