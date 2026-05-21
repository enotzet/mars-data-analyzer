package app.service;

import app.dto.BenchmarkResultDto;
import app.dto.BenchmarkResultDto.ModeResult;
import app.dto.BenchmarkResultDto.QueryStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Benchmark service implementing a defensible RAG evaluation methodology:
 *
 *   1. Four-level baseline ladder: ZERO_SHOT, SYSTEM_PROMPT, RAG_BASELINE,
 *      RAG_RERANKED — instead of a 2-mode comparison.
 *   2. Statistical significance: each (query, mode) pair is executed N times
 *      (default 5) and reported as mean ± standard deviation.
 *   3. LLM-as-a-Judge: each generated answer is compared against a manually
 *      curated ideal answer using a separate LLM call that returns an integer
 *      score on a 0–5 scale, tolerant of synonyms and paraphrasing.
 *   4. Precision@K is retained as a secondary, retrieval-only diagnostic but
 *      is no longer the headline metric.
 */
@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);
    private static final int DEFAULT_REPEATS = 5;
    private static final Pattern FIRST_INT = Pattern.compile("[0-5]");

    private final RetrievalService retrievalService;
    private final MarsService marsService;
    private final PromptService promptService;
    private final ChatModel chatModel;

    /**
     * Twenty Mars-related queries paired with (a) expected keywords for the
     * legacy Precision@K metric and (b) a manually authored ideal answer used
     * as ground truth by the LLM-as-a-Judge metric. The ideal answers were
     * authored from public planetary-science sources before any benchmark run.
     */
    private static final List<BenchmarkQuery> BENCHMARK_QUERIES = List.of(
        new BenchmarkQuery(
            "What geological features are visible on the Martian surface?",
            Set.of("rock", "crater", "sediment", "geological", "terrain"),
            "The Martian surface displays a wide range of geological features including impact craters of every size, sedimentary rock layers, basaltic lava plains, sand dunes shaped by wind, valley networks carved by ancient water flow, and large tectonic systems such as Valles Marineris. The northern hemisphere is dominated by smooth lowlands while the southern hemisphere is heavily cratered highland terrain."),
        new BenchmarkQuery(
            "Describe the color of Mars soil",
            Set.of("red", "iron", "oxide", "soil", "dust", "color"),
            "Martian soil and surface dust appear reddish-brown to orange because they are rich in iron oxides, particularly hematite and other ferric oxides that have formed by oxidation. Closer-up rover images also reveal grey, tan, and locally bluish basaltic materials beneath the dust."),
        new BenchmarkQuery(
            "What evidence of water exists on Mars?",
            Set.of("water", "ice", "erosion", "channel", "mineral"),
            "Evidence of water on Mars includes ancient river valley networks, outflow channels, sedimentary layered deposits, hydrated minerals such as clays and sulfates detected by orbital and rover spectrometers, recurring slope lineae, polar water-ice caps, and subsurface ice detected by radar. Past Mars therefore had liquid surface water; present-day water exists primarily as subsurface ice and as polar caps."),
        new BenchmarkQuery(
            "What does the Martian sky look like?",
            Set.of("sky", "atmosphere", "dust", "sunset", "blue"),
            "During the day the Martian sky appears butterscotch or pinkish-tan because suspended dust scatters red light. Around sunrise and sunset the sky near the Sun takes on a distinctive blue tint, the inverse of Earth's reddish sunsets, because fine dust forward-scatters blue wavelengths."),
        new BenchmarkQuery(
            "Describe craters on Mars surface",
            Set.of("crater", "impact", "rim", "basin"),
            "Mars is heavily cratered, especially in its southern highlands. Craters range from small simple bowl-shaped craters to large complex craters with central peaks, terraced rims, and ejecta blankets. Some craters show distinctive lobate ejecta indicating subsurface volatiles, and the largest impact basins (Hellas, Argyre, Utopia) span thousands of kilometres."),
        new BenchmarkQuery(
            "What rocks are found on Mars?",
            Set.of("rock", "basalt", "mineral", "volcanic", "igneous"),
            "Mars's surface rocks are dominantly basaltic igneous rocks rich in plagioclase, pyroxene, and olivine. Sedimentary rocks including mudstones, sandstones, and conglomerates have been documented by Curiosity in Gale Crater. Hydrated minerals such as clays, sulfates, and hematite are also present, indicating past aqueous alteration."),
        new BenchmarkQuery(
            "How does Mars terrain compare to Earth?",
            Set.of("terrain", "earth", "mountain", "valley", "comparison"),
            "Mars has the largest volcano in the Solar System (Olympus Mons, ~22 km tall), the largest canyon system (Valles Marineris, ~4000 km long), a global hemispheric dichotomy between low northern plains and cratered southern highlands, no plate tectonics, no surface oceans, and a much thinner atmosphere than Earth. Surface gravity is about 38% of Earth's."),
        new BenchmarkQuery(
            "What is the composition of Martian dust?",
            Set.of("dust", "iron", "oxide", "particle", "composition"),
            "Martian dust is fine-grained, micrometre-sized, and consists mainly of weathered basaltic material rich in iron oxides (hematite, magnetite) which give it the red colour. It also contains silicates, sulfates, and small amounts of perchlorate salts, and is globally homogenised by frequent dust storms."),
        new BenchmarkQuery(
            "Describe sand dunes on Mars",
            Set.of("sand", "dune", "wind", "aeolian", "pattern"),
            "Mars hosts extensive aeolian dune fields, particularly in crater floors and around the polar caps. The dunes are predominantly basaltic, often dark, and exhibit barchan, transverse, and star morphologies shaped by prevailing winds. Many are demonstrably active today, migrating slowly under contemporary Martian winds."),
        new BenchmarkQuery(
            "What volcanic features exist on Mars?",
            Set.of("volcano", "olympus", "lava", "volcanic", "caldera"),
            "Mars has enormous shield volcanoes concentrated in the Tharsis and Elysium regions, including Olympus Mons (about 22 km high) and the Tharsis Montes. Features include broad shield slopes, summit calderas, lava flows, lava tubes and pit chains, and tuff cones. Mars lacks plate tectonics, so volcanoes grew over long-lived stationary mantle plumes."),
        new BenchmarkQuery(
            "Describe the Martian polar ice caps",
            Set.of("ice", "polar", "cap", "carbon", "dioxide", "water"),
            "Mars has perennial polar caps composed primarily of water ice with a seasonal overlay of carbon dioxide ice that grows in winter and sublimates in summer. The northern cap is larger and dominated by water ice; the southern cap retains a thicker permanent CO2 layer year round. The caps record climate history in their layered deposits."),
        new BenchmarkQuery(
            "What weather patterns occur on Mars?",
            Set.of("weather", "storm", "dust", "wind", "temperature"),
            "Martian weather is dominated by large daily temperature swings (often more than 60°C), persistent low-pressure conditions, seasonal cycling of CO2 between atmosphere and polar caps, dust devils, and dust storms that can occasionally grow to engulf the entire planet. Surface wind speeds are typically modest because the atmosphere is thin."),
        new BenchmarkQuery(
            "Describe Valles Marineris canyon system",
            Set.of("valley", "canyon", "marineris", "rift", "tectonic"),
            "Valles Marineris is a system of interconnected canyons stretching about 4000 km along the Martian equator, up to 200 km wide and 7 km deep. It is primarily of tectonic origin, formed by extensional faulting associated with the rise of the Tharsis bulge, and was subsequently widened by landslides, water erosion, and collapse."),
        new BenchmarkQuery(
            "What minerals have been found on Mars?",
            Set.of("mineral", "hematite", "olivine", "sulfate", "clay"),
            "Mars contains primary igneous minerals such as olivine, pyroxene, and plagioclase, plus secondary minerals indicative of aqueous alteration including hematite, magnetite, smectite and other clay minerals, sulfates such as gypsum and jarosite, and carbonates. Perchlorate salts are also widespread in the soil."),
        new BenchmarkQuery(
            "How does the Curiosity rover navigate Mars terrain?",
            Set.of("rover", "curiosity", "wheel", "navigate", "terrain"),
            "Curiosity navigates using six aluminium wheels on a rocker-bogie suspension, hazard cameras for obstacle avoidance, navigation cameras for path planning, and onboard autonomous-navigation software that builds a local terrain map. Drives are planned daily by Earth-based operators based on imagery from the previous sol."),
        new BenchmarkQuery(
            "What does the Mars surface look like at night?",
            Set.of("night", "dark", "star", "moon", "phobos"),
            "At night the Martian surface is very dark and cold, illuminated only by stars and by the two small moons Phobos and Deimos. Phobos crosses the sky multiple times per night, while Deimos appears more like a bright star. Surface temperatures can drop below −90°C in equatorial regions and below −120°C at the poles."),
        new BenchmarkQuery(
            "Describe sedimentary layers on Mars",
            Set.of("sediment", "layer", "strata", "deposit", "formation"),
            "Sedimentary layered rocks have been documented at many sites on Mars, most prominently in Gale Crater where Curiosity has examined hundreds of metres of strata. The layers record episodic deposition by water, wind, and volcanic processes and contain mudstones, sandstones, and cross-bedded sediments that imply lacustrine and fluvial environments."),
        new BenchmarkQuery(
            "What is the temperature on Mars surface?",
            Set.of("temperature", "cold", "celsius", "fahrenheit", "climate"),
            "Surface temperatures on Mars typically range from about −140°C at the winter poles to roughly +20°C on the warmest equatorial summer afternoons. The global mean surface temperature is approximately −63°C. Daily swings often exceed 60°C because the thin atmosphere retains little heat."),
        new BenchmarkQuery(
            "Describe the Gale Crater exploration",
            Set.of("gale", "crater", "curiosity", "mount", "sharp"),
            "Gale Crater is a 154 km diameter impact crater that has been explored by NASA's Curiosity rover since August 2012. Its central peak Mount Sharp (Aeolis Mons) preserves an ascending sequence of sedimentary layers that record a long history of past lake environments, providing key evidence for habitable conditions on early Mars."),
        new BenchmarkQuery(
            "What signs of past habitability exist on Mars?",
            Set.of("habitable", "organic", "life", "biosignature", "water"),
            "Signs of past habitability on Mars include evidence of long-lived liquid water (lakes, river deltas, hydrated minerals), organic molecules detected by Curiosity in Gale Crater mudstones, seasonal methane fluctuations of unknown origin, and chemical energy sources usable by microbial metabolism. No biosignatures confirming actual past life have yet been found.")
    );

    public BenchmarkService(RetrievalService retrievalService,
                            MarsService marsService,
                            PromptService promptService,
                            ChatModel chatModel) {
        this.retrievalService = retrievalService;
        this.marsService = marsService;
        this.promptService = promptService;
        this.chatModel = chatModel;
    }

    /**
     * Run the full four-mode benchmark with N repeats per (query, mode) pair.
     * Default N = 5.
     */
    public BenchmarkResultDto runFullBenchmark(int repeats) {
        int n = repeats > 0 ? repeats : DEFAULT_REPEATS;
        String runId = UUID.randomUUID().toString();
        log.info("Starting full benchmark run {} with N={} repeats", runId, n);

        List<MarsService.BenchmarkMode> modes = List.of(
                MarsService.BenchmarkMode.ZERO_SHOT,
                MarsService.BenchmarkMode.SYSTEM_PROMPT,
                MarsService.BenchmarkMode.RAG_BASELINE,
                MarsService.BenchmarkMode.RAG_RERANKED
        );

        List<ModeResult> modeResults = new ArrayList<>();
        for (MarsService.BenchmarkMode mode : modes) {
            log.info("Mode {}: running {} queries × {} repeats", mode, BENCHMARK_QUERIES.size(), n);
            modeResults.add(runMode(mode, n));
        }

        return new BenchmarkResultDto(runId, BENCHMARK_QUERIES.size(), n, modeResults);
    }

    /** Backwards-compatible single-mode endpoint (used by older callers/tests). */
    public ModeResult runSingleMode(MarsService.BenchmarkMode mode, int repeats) {
        int n = repeats > 0 ? repeats : DEFAULT_REPEATS;
        return runMode(mode, n);
    }

    private ModeResult runMode(MarsService.BenchmarkMode mode, int repeats) {
        List<QueryStats> perQuery = new ArrayList<>();
        List<Double> allJudge = new ArrayList<>();
        List<Double> allPrec = new ArrayList<>();
        List<Double> allLat = new ArrayList<>();
        int withSources = 0;

        for (BenchmarkQuery bq : BENCHMARK_QUERIES) {
            double[] judges = new double[repeats];
            double[] precs = new double[repeats];
            double[] lats = new double[repeats];
            String exampleAnswer = "";
            int sourcesSeen = 0;

            for (int r = 0; r < repeats; r++) {
                long t0 = System.currentTimeMillis();
                MarsService.BenchmarkAnswer ba;
                try {
                    ba = marsService.askInMode(bq.query(), mode);
                } catch (Exception e) {
                    log.warn("askInMode failed for query='{}' mode={} repeat={}: {}",
                            bq.query(), mode, r, e.getMessage());
                    judges[r] = 0;
                    precs[r] = 0;
                    lats[r] = System.currentTimeMillis() - t0;
                    continue;
                }
                long latency = System.currentTimeMillis() - t0;
                lats[r] = latency;
                if (ba.sourcesUsed() > 0) sourcesSeen = ba.sourcesUsed();
                if (r == 0) exampleAnswer = ba.answer().substring(0, Math.min(220, ba.answer().length()));

                // LLM-as-a-Judge: semantic comparison against ground truth
                judges[r] = judgeAnswer(bq.query(), bq.idealAnswer(), ba.answer());

                // Legacy keyword Precision@K — only meaningful for RAG modes (where retrieval happened)
                if (mode == MarsService.BenchmarkMode.RAG_BASELINE || mode == MarsService.BenchmarkMode.RAG_RERANKED) {
                    var docs = (mode == MarsService.BenchmarkMode.RAG_RERANKED)
                            ? retrievalService.hybridSearch(bq.query(), 3)
                            : retrievalService.baselineSearch(bq.query(), 3);
                    int relevant = 0;
                    for (var sd : docs) {
                        String content = sd.document().getContent().toLowerCase();
                        long matchCount = bq.expectedKeywords().stream()
                                .filter(kw -> content.contains(kw.toLowerCase()))
                                .count();
                        if (matchCount >= 2) relevant++;
                    }
                    precs[r] = docs.isEmpty() ? 0.0 : relevant / (double) docs.size();
                } else {
                    precs[r] = Double.NaN; // not applicable for non-RAG modes
                }
            }

            if (sourcesSeen > 0) withSources++;

            double meanJ = mean(judges);
            double stdJ = std(judges, meanJ);
            double meanP = nanAwareMean(precs);
            double meanL = mean(lats);
            double stdL = std(lats, meanL);

            perQuery.add(new QueryStats(bq.query(), meanJ, stdJ, meanP, meanL, stdL, repeats, exampleAnswer));

            for (double j : judges) allJudge.add(j);
            for (double p : precs) if (!Double.isNaN(p)) allPrec.add(p);
            for (double l : lats) allLat.add(l);
        }

        double avgJ = allJudge.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdJAll = std(allJudge, avgJ);
        double avgP = allPrec.isEmpty() ? Double.NaN
                : allPrec.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdPAll = allPrec.isEmpty() ? Double.NaN : std(allPrec, avgP);
        double avgL = allLat.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdLAll = std(allLat, avgL);
        double consistency = withSources / (double) BENCHMARK_QUERIES.size();

        return new ModeResult(
                mode.name().toLowerCase().replace('_', '-'),
                avgJ, stdJAll,
                avgP, stdPAll,
                avgL, stdLAll,
                consistency,
                perQuery
        );
    }

    /**
     * LLM-as-a-Judge: ask the chat model to score the assistant's answer
     * against the manually authored ideal answer on a 0–5 integer scale.
     * Returns 0 if the call fails or the response cannot be parsed.
     */
    private double judgeAnswer(String question, String ideal, String actual) {
        try {
            String tplText = promptService.getActivePrompt(PromptService.JUDGE).getTemplateText();
            SystemPromptTemplate tpl = new SystemPromptTemplate(tplText);
            Prompt sysPrompt = tpl.create(Map.of(
                    "question", question == null ? "" : question,
                    "ideal", ideal == null ? "" : ideal,
                    "actual", actual == null ? "" : actual
            ));
            Prompt prompt = new Prompt(
                    List.of(sysPrompt.getInstructions().get(0), new UserMessage("Provide the score now.")),
                    OpenAiChatOptions.builder()
                            .withTemperature(0.0F)
                            .withMaxTokens(60)
                            .build());
            String reply = chatModel.call(prompt).getResult().getOutput().getContent();
            var m = FIRST_INT.matcher(reply == null ? "" : reply.trim());
            if (m.find()) {
                int score = Integer.parseInt(m.group());
                return Math.max(0, Math.min(5, score));
            }
            log.warn("Judge returned no parseable score: {}", reply);
            return 0.0;
        } catch (Exception e) {
            log.warn("Judge call failed: {}", e.getMessage());
            return 0.0;
        }
    }

    private static double mean(double[] xs) {
        if (xs.length == 0) return 0;
        double s = 0;
        for (double x : xs) s += x;
        return s / xs.length;
    }

    private static double std(double[] xs, double mean) {
        if (xs.length < 2) return 0;
        double s = 0;
        for (double x : xs) s += (x - mean) * (x - mean);
        return Math.sqrt(s / (xs.length - 1));
    }

    private static double std(List<Double> xs, double mean) {
        if (xs.size() < 2) return 0;
        double s = 0;
        for (double x : xs) s += (x - mean) * (x - mean);
        return Math.sqrt(s / (xs.size() - 1));
    }

    private static double nanAwareMean(double[] xs) {
        double s = 0; int n = 0;
        for (double x : xs) if (!Double.isNaN(x)) { s += x; n++; }
        return n == 0 ? Double.NaN : s / n;
    }

    private record BenchmarkQuery(String query, Set<String> expectedKeywords, String idealAnswer) {}
}
