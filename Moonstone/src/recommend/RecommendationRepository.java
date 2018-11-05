package recommend;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by florian on 11/14/16.
 */
class RecommendationRepository {

    final Map<String, Map<String, List<RelevantHandler>>> catchRecommendations;
    final Map<String, List<RelevantHandler>> finallyRecommendations;

    RecommendationRepository() {
        catchRecommendations = new HashMap<>();
        finallyRecommendations = new HashMap<>();
    }

    RecommendationRepository(RecommendationRepository from) {
        catchRecommendations = Collections.unmodifiableMap(from.catchRecommendations);
        finallyRecommendations = Collections.unmodifiableMap(from.finallyRecommendations);
    }

}
