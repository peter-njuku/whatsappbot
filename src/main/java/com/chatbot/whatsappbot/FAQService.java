package com.chatbot.whatsappbot;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class FAQService {

    private final List<FAQEntry> knowledgeBase;
    private final ConcurrentHashMap<String, String> responseCache;
    private static final LevenshteinDistance LEVENSHTEIN = LevenshteinDistance.getDefaultInstance();
    private static final double MATCH_THRESHOLD = 0.1;

    public FAQService() {
        this.knowledgeBase = createFAQDatabase();
        this.responseCache = new ConcurrentHashMap<>();
        warmCache();
        System.out.println("✅ Loaded " + knowledgeBase.size() + " FAQ categories");
    }

    private List<FAQEntry> createFAQDatabase() {
        return List.of(
                // 1. Academic Programs and Courses
                new FAQEntry(
                        "academic_programs",
                        List.of(
                                new Keyword("course requirements major", 2.0),
                                new Keyword("change major minor", 1.9),
                                new Keyword("extra units semester", 1.8),
                                new Keyword("class timetable released", 1.9),
                                new Keyword("recommended electives", 1.7),
                                new Keyword("audit class another department", 1.6),
                                new Keyword("course not offered", 1.8),
                                new Keyword("request new course", 1.5),
                                new Keyword("course full", 1.9),
                                new Keyword("transfer credit another university", 1.7)
                        ), """
                           Academic Programs Info:
                           - Course requirements: Consult your program handbook
                           - Major changes: Submit Form A-2 to Dean's Office
                           - Timetable release: 2 weeks before semester start
                           - Course full? Join waitlist at registrar office""",
                        "Are you asking about program requirements, course changes, or timetables?"
                ),
                // 2. Examinations and Grades
                new FAQEntry(
                        "examinations",
                        List.of(
                                new Keyword("appeal grade", 2.0),
                                new Keyword("supplementary special exam", 1.9),
                                new Keyword("miss exam illness", 2.0),
                                new Keyword("view marked exam script", 1.7),
                                new Keyword("class average GPA calculated", 1.6)
                        ),
                        """
                        Examinations Office:
                        - Grade appeals: Within 14 days via Form E-1
                        - Missed exams: Medical certificate required
                        - Script viewing: Room 205, by appointment
                        - GPA formula: (Σ Grade Points × Credits) / Total Credits
                        """,
                        "Need help with grades, exams, or GPA calculations?"
                ),
                // 3. Registration and Administration
                new FAQEntry(
                        "registration",
                        List.of(
                                new Keyword("register drop course", 2.0),
                                new Keyword("can't register online", 1.9),
                                new Keyword("defer studies procedure", 1.8),
                                new Keyword("request academic transcripts", 1.7),
                                new Keyword("recommendation letter department", 1.6)
                        ),
                        """
                        Registration Help:
                        - Add/drop: First 2 weeks via student portal
                        - Portal issues? Visit IT helpdesk
                        - Transcripts: Allow 3 business days
                        - Recommendation letters: Request 3 weeks in advance
                        """,
                        "Is this about registration, transcripts, or recommendation letters?"
                ),
                // 4. Projects and Research
                new FAQEntry(
                        "research",
                        List.of(
                                new Keyword("choose final year project supervisor", 1.9),
                                new Keyword("research funding opportunities", 2.0),
                                new Keyword("thesis submission guidelines", 1.8),
                                new Keyword("change project research topic", 1.7),
                                new Keyword("student research conferences", 1.6)
                        ), """
                           Research Support:
                           - Supervisor selection: Review faculty profiles
                           - Funding deadlines: March 1 and October 1
                           - Thesis format: Download template from dept website
                           - Conferences: Annual showcase in November""",
                        "Are you inquiring about research projects, funding, or thesis guidelines?"
                ),
                // 5. Internship and Career
                new FAQEntry(
                        "internships",
                        List.of(
                                new Keyword("internship compulsory", 1.9),
                                new Keyword("department help find attachment", 2.0),
                                new Keyword("career support services", 1.8),
                                new Keyword("letter introduction internship", 1.7),
                                new Keyword("job placement partnerships", 1.6)
                        ), """
                           Career Services:
                           - Mandatory for: Business (200hrs), Engineering (300hrs)
                           - Portal: careers.uni.edu/placements
                           - Interview prep: Every Tuesday 2-4pm
                           - Employer partnerships: 50+ companies registered""",
                        "Need internship or career guidance?"
                ),
                // 6. Policies and Discipline
                new FAQEntry(
                        "policies",
                        List.of(
                                new Keyword("plagiarism policy", 2.0),
                                new Keyword("class absenteeism actions", 1.8),
                                new Keyword("academic dishonesty cases", 2.0),
                                new Keyword("discrimination harassment", 1.9),
                                new Keyword("academic probation policy", 1.7)
                        ), """
                           Policies:
                           - Plagiarism: First offense = course failure
                           - >30% absences: Automatic grade penalty
                           - Harassment reports: confidential@uni.edu
                           - Probation: GPA <2.0 for 2 consecutive terms""",
                        "Querying policies? Specify: plagiarism, attendance, or complaints?"
                ),
                // 7. Student Support
                new FAQEntry(
                        "student_support",
                        List.of(
                                new Keyword("mentorship programs", 1.7),
                                new Keyword("concern about lecturer", 1.9),
                                new Keyword("struggling academically", 2.0),
                                new Keyword("counseling wellness services", 1.8),
                                new Keyword("feedback course department", 1.6)
                        ), """
                           Support Services:
                           - Mentors: Sign up at studentlife.uni.edu
                           - Lecturer concerns: Submit Form F-12 anonymously
                           - Academic help: Free tutoring in Library
                           - Crisis line: 24/7 at 555-HELP""",
                        "Need support? Ask about mentoring, counseling, or academic help."
                )
        );
    }

    public String findBestAnswer(String userQuery) {
        String cleanQuery = normalizeInput(userQuery);

        // Debug output
        System.out.println("\n=== New Query ===");
        System.out.println("Original: " + userQuery);
        System.out.println("Cleaned: " + cleanQuery);

        // Check cache
        String cached = responseCache.get(cleanQuery);
        if (cached != null) {
            System.out.println("Served from cache");
            return cached;
        }

        // Find best match with debug
        Optional<FAQMatch> bestMatch = knowledgeBase.stream()
                .peek(entry -> System.out.println("\nChecking category: " + entry.category()))
                .map(entry -> {
                    double score = calculateMatchScore(entry, cleanQuery);
                    System.out.printf("Top keyword score: %.2f%n", score);
                    return new FAQMatch(entry, score);
                })
                .filter(match -> {
                    boolean passes = match.score() >= MATCH_THRESHOLD;
                    System.out.printf("Threshold check: %.2f %s%n",
                            match.score(), passes ? "✅" : "❌");
                    return passes;
                })
                .max((a, b) -> Double.compare(a.score(), b.score()));

        String response = bestMatch.map(match -> {
            System.out.printf("Selected: %s (Score: %.2f)%n",
                    match.entry().category(), match.score());
            return match.entry().answer();
        })
                .orElseGet(() -> {
                    System.out.println("No good matches, using fallback");
                    return getFallbackResponse(cleanQuery);
                });

        responseCache.put(cleanQuery, response);
        return response;
    }

    private double calculateMatchScore(FAQEntry entry, String query) {
        return entry.keywords().stream()
                .mapToDouble(keyword -> {
                    // Split multi-word keywords
                    String[] parts = keyword.text().toLowerCase().split(" ");

                    // Check if ANY parts exist in query (OR logic)
                    boolean anyPartsMatch = Arrays.stream(parts)
                            .anyMatch(part -> query.contains(part));

                    if (!anyPartsMatch) return 0.0;

                    // Calculate similarity for full phrase
                    int distance = LEVENSHTEIN.apply(keyword.text().toLowerCase(), query);
                    double similarity = 1 - ((double) distance
                            / Math.max(keyword.text().length(), query.length()));

                    double weightedScore = similarity * keyword.weight();

                    System.out.printf("  Keyword '%s' → %.2f (weighted: %.2f)%n",
                            keyword.text(), similarity, weightedScore);

                    return weightedScore;
                })
                .max()
                .orElse(0.0);
    }

    private String getFallbackResponse(String query) {
        return knowledgeBase.stream()
                .filter(entry -> entry.fallback() != null)
                .max((a, b) -> Double.compare(
                calculateMatchScore(a, query),
                calculateMatchScore(b, query)
        ))
                .map(FAQEntry::fallback)
                .orElse("I didn't understand. Try asking about:\n- Courses\n- Exams\n- Registration");
    }

    private void warmCache() {
        knowledgeBase.forEach(entry -> {
            entry.keywords().forEach(keyword -> {
                responseCache.put(keyword.text().toLowerCase(), entry.answer());
            });
        });
    }

    private String normalizeInput(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // Records (Java 16+) for immutable data
    public record FAQEntry(
            String category,
            List<Keyword> keywords,
            String answer,
            String fallback
            ) {

    }

    public record Keyword(
            String text,
            double weight
            ) {

    }

    public record FAQMatch(
            FAQEntry entry,
            double score
            ) {

    }
}
