package com.worklink.service.semantic;

import com.worklink.dto.matching.SemanticMatchResult;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Deterministic lexical/statistical baseline; this is not transformer semantic understanding. */
@Component
public class TfidfCosineSemanticRelevanceEngine implements SemanticRelevanceEngine {
    private static final Pattern TOKEN_BOUNDARY = Pattern.compile("[^\\p{L}\\p{N}+#]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "been", "being", "but", "by", "for", "from",
            "had", "has", "have", "he", "her", "hers", "him", "his", "i", "in", "into", "is", "it",
            "its", "me", "my", "of", "on", "or", "our", "ours", "she", "that", "the", "their", "them",
            "there", "these", "they", "this", "those", "to", "us", "was", "we", "were", "with", "you", "your");

    @Override
    public SemanticMatchResult evaluate(Long projectId, Long freelancerId,
            List<SemanticTextField> projectFields, List<SemanticTextField> freelancerFields) {
        Map<String, Double> projectTf = termFrequency(projectFields);
        Map<String, Double> freelancerTf = termFrequency(freelancerFields);
        if (projectTf.isEmpty() || freelancerTf.isEmpty()) {
            return SemanticMatchResult.unavailable(projectId, freelancerId,
                    "Semantic relevance is unavailable because one or both profiles have no usable text.");
        }

        Set<String> vocabulary = new HashSet<>(projectTf.keySet());
        vocabulary.addAll(freelancerTf.keySet());
        Map<String, Double> projectVector = tfIdf(projectTf, vocabulary, freelancerTf);
        Map<String, Double> freelancerVector = tfIdf(freelancerTf, vocabulary, projectTf);
        double projectNorm = norm(projectVector);
        double freelancerNorm = norm(freelancerVector);
        if (projectNorm == 0.0 || freelancerNorm == 0.0) {
            return SemanticMatchResult.unavailable(projectId, freelancerId,
                    "Semantic relevance is unavailable because one or both profiles have no usable terms.");
        }

        double dot = vocabulary.stream().mapToDouble(term ->
                projectVector.getOrDefault(term, 0.0) * freelancerVector.getOrDefault(term, 0.0)).sum();
        double similarity = Math.max(0.0, Math.min(1.0, dot / (projectNorm * freelancerNorm)));
        List<String> projectTerms = orderedTerms(projectVector);
        List<String> freelancerTerms = orderedTerms(freelancerVector);
        List<String> sharedTerms = vocabulary.stream()
                .filter(term -> projectVector.getOrDefault(term, 0.0) > 0 && freelancerVector.getOrDefault(term, 0.0) > 0)
                .sorted(Comparator.comparingDouble((String term) -> Math.min(projectVector.get(term), freelancerVector.get(term))).reversed())
                .limit(5).toList();
        String explanation = "Semantic relevance is a lexical TF-IDF/cosine comparison of project requirements and freelancer profile, skills, and portfolio text."
                + (sharedTerms.isEmpty() ? " No terms overlap." : " Shared terms: " + String.join(", ", sharedTerms) + ".");
        return new SemanticMatchResult(projectId, freelancerId, similarity, similarity * 100.0,
                "AVAILABLE", projectTerms, freelancerTerms, explanation);
    }

    private Map<String, Double> termFrequency(List<SemanticTextField> fields) {
        Map<String, Double> frequencies = new HashMap<>();
        if (fields == null) return frequencies;
        for (SemanticTextField field : fields) {
            if (field == null || field.text() == null || field.text().isBlank() || !Double.isFinite(field.weight()) || field.weight() <= 0) continue;
            String normalized = Normalizer.normalize(field.text(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
            for (String token : TOKEN_BOUNDARY.split(normalized)) {
                String term = token.trim();
                if (term.length() < 2 || STOP_WORDS.contains(term)) continue;
                frequencies.merge(term, field.weight(), Double::sum);
            }
        }
        double total = frequencies.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) frequencies.replaceAll((term, frequency) -> frequency / total);
        return frequencies;
    }

    private Map<String, Double> tfIdf(Map<String, Double> tf, Set<String> vocabulary, Map<String, Double> other) {
        Map<String, Double> vector = new HashMap<>();
        for (String term : vocabulary) {
            Double frequency = tf.get(term);
            if (frequency == null) continue;
            int documentFrequency = 1 + (other.containsKey(term) ? 1 : 0);
            double inverseDocumentFrequency = Math.log(3.0 / (documentFrequency + 1.0)) + 1.0;
            vector.put(term, frequency * inverseDocumentFrequency);
        }
        return vector;
    }

    private double norm(Map<String, Double> vector) {
        return Math.sqrt(vector.values().stream().mapToDouble(value -> value * value).sum());
    }

    private List<String> orderedTerms(Map<String, Double> vector) {
        return vector.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
    }
}
