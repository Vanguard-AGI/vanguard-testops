package io.vanguard.testops.functional.service;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 代码复杂度评估器（与 Python 参考实现对齐）
 * 基于静态代码特征：API/DB/SQL/逻辑/MQ/高风险/链式调用/函数定义等
 */
@Slf4j
public class CodeComplexityEvaluator {

    private static final int BASE_SCORE = 1;
    private static final double WEIGHT_API_CALLS = 2;
    private static final double WEIGHT_DB_OPS = 1;
    private static final double WEIGHT_LOGIC_BRANCHES = 0.7;
    private static final double WEIGHT_MQ_OPS = 5;
    private static final double WEIGHT_HIGH_RISK = 4;
    private static final double WEIGHT_CHAIN_CALLS = 2;
    private static final double WEIGHT_FUNCTION_DEFS = 1;
    private static final double WEIGHT_SQL_STATEMENTS = 0.7;
    private static final double FUNCTION_CAP = 20;

    /**
     * 评估代码复杂度
     *
     * @param code 代码内容（可为 null）
     * @return 包含 level、scores（base/api/db/sql/logic/mq/risk/chain/functions）、total_cs 的 Map
     */
    public static Map<String, Object> evaluateComplexity(String code) {
        if (code == null || code.trim().isEmpty()) {
            return createDefaultResult();
        }
        String cleanText = cleanCode(code);

        // 特征计数
        int apiCount = countMatches(cleanText, new String[]{
                "(?:requests|client|session|http)\\.(?:get|post|request)\\(",
                "await\\s+.*?(?:invoke|get|post)\\("
        });
        int dbCount = countMatches(cleanText, new String[]{
                "\\.(?:execute|commit|rollback|query|add|delete|insert|update|select)\\(",
                "\\bcursor\\.execute\\b",
                "\\bsql_execute\\b",
                "\\bsql_search\\b"
        });
        int sqlCount = countMatches(cleanText, new String[]{
                "\\binsert\\s+into\\b",
                "\\bdelete\\s+from\\b",
                "\\bupdate\\s+\\w+\\s+set\\b",
                "\\bselect\\s+.*?\\s+from\\b"
        });
        int logicCount = countMatches(cleanText, new String[]{
                "\\bif\\s+", "\\belif\\s+", "\\bfor\\s+", "\\bwhile\\s+",
                "\\belse\\s*:", "\\bmatch\\s+", "\\bcase\\s+"
        });
        int mqCount = countMatches(cleanText, new String[]{
                "\\.(?:send_message|publish|send|produce)\\s*\\(",
                "\\b(?:kafka|rabbitmq|rocketmq|activemq)\\.|\\.(?:kafka|rabbitmq|rocketmq|activemq)\\b",
                "\\b(?:redis\\.publish|redis\\.pubsub)\\b",
                "\\.cache\\.(?:set|put|store)\\s*\\(",
                "\\b(?:push_msg|send_mq|mq_send)\\s*\\(",
                "\"(?:topic|queue|exchange)\"\\s*[:,]",
                "\\.post\\s*\\([^)]*(?:mq|message|topic)"
        });
        int highRiskCount = countMatches(cleanText, new String[]{
                "\\.transaction\\(", "@transaction", "\\bwith\\s+transaction",
                "\\bmock\\.", "side_effect",
                "\\bthreading\\.", "\\basyncio\\.", "await\\s+gather",
                "\\bxxl_job\\b", "\\bexecutor_handler\\b",
                "\\w*risk\\w*\\(",
                "\\bos\\.(?:path\\.)?(?:exists|read|write|remove|join|rename|mkdir|rmdir)\\w*\\(",
                "(?:\\bpd\\.|pandas\\.)(?:read_|to_)\\w*\\("
        });
        int functionCount = countMatches(cleanText, new String[]{
                "\\bdef\\s+\\w+\\s*\\([^)]*\\)"
        });
        int chainCount = countMatches(cleanText, new String[]{
                "\\.\\w+\\([^)]*\\)\\.\\w+\\("
        });

        Map<String, Object> scores = new HashMap<>();
        scores.put("base", BASE_SCORE);
        scores.put("api", apiCount * WEIGHT_API_CALLS);
        scores.put("db", dbCount * WEIGHT_DB_OPS);
        scores.put("sql", Math.round(sqlCount * WEIGHT_SQL_STATEMENTS * 100.0) / 100.0);
        scores.put("logic", Math.round(logicCount * WEIGHT_LOGIC_BRANCHES * 100.0) / 100.0);
        scores.put("mq", mqCount * WEIGHT_MQ_OPS);
        scores.put("risk", highRiskCount * WEIGHT_HIGH_RISK);
        scores.put("chain", chainCount * WEIGHT_CHAIN_CALLS);
        scores.put("functions", Math.min(functionCount * WEIGHT_FUNCTION_DEFS, FUNCTION_CAP));

        double totalCsDouble = ((Number) scores.get("base")).doubleValue()
                + ((Number) scores.get("api")).doubleValue()
                + ((Number) scores.get("db")).doubleValue()
                + ((Number) scores.get("sql")).doubleValue()
                + ((Number) scores.get("logic")).doubleValue()
                + ((Number) scores.get("mq")).doubleValue()
                + ((Number) scores.get("risk")).doubleValue()
                + ((Number) scores.get("chain")).doubleValue()
                + ((Number) scores.get("functions")).doubleValue();
        int totalCs = (int) totalCsDouble;

        String level = determineLevel(totalCs, highRiskCount, logicCount, apiCount);

        Map<String, Object> result = new HashMap<>();
        result.put("level", level);
        result.put("scores", scores);
        result.put("total_cs", (double) totalCs);
        return result;
    }

    private static String cleanCode(String text) {
        if (text == null) return "";
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            int hash = line.indexOf('#');
            String stripped = (hash >= 0 ? line.substring(0, hash) : line).trim();
            if (!stripped.isEmpty()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(stripped.toLowerCase());
            }
        }
        return sb.toString();
    }

    private static int countMatches(String text, String[] patterns) {
        if (text == null) return 0;
        int count = 0;
        for (String p : patterns) {
            Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = pattern.matcher(text);
            while (m.find()) count++;
        }
        return count;
    }

    /**
     * 判定等级（与 Python 一致）
     */
    private static String determineLevel(int totalCs, int highRiskCount, int logicCount, int apiCount) {
        if (totalCs > 190 && highRiskCount > 20) return "D6";
        if (totalCs > 165 && logicCount > 80) return "D5";
        if (totalCs > 115 && apiCount >= 5) return "D4";
        if (totalCs > 90 && highRiskCount > 1) return "D3";
        if (totalCs > 60) return "D2";
        if (totalCs > 35) return "D1";
        return "D0";
    }

    private static Map<String, Object> createDefaultResult() {
        Map<String, Object> scores = new HashMap<>();
        scores.put("base", 1);
        scores.put("api", 0);
        scores.put("db", 0);
        scores.put("sql", 0);
        scores.put("logic", 0);
        scores.put("mq", 0);
        scores.put("risk", 0);
        scores.put("chain", 0);
        scores.put("functions", 0);
        Map<String, Object> result = new HashMap<>();
        result.put("level", "D0");
        result.put("scores", scores);
        result.put("total_cs", 1);
        return result;
    }
}
