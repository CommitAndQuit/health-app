package io.biotrace.app.domain.analytics.clustering

import java.time.LocalDate
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pure Kotlin on-device unsupervised K-Means clustering engine for daily lifestyle vectors.
 *
 * Normalizes input features via Min-Max scaling, runs iterative centroid optimization
 * with convergence detection, and dynamically maps cluster centroids to clinically meaningful
 * [LifestyleArchetype] classifications.
 */
@Singleton
class LifestyleKMeansClusterer @Inject constructor() {

    companion object {
        private const val FEATURE_COUNT = 5
        private const val MAX_ITERATIONS = 100
        private const val CONVERGENCE_EPSILON = 1e-4
    }

    /**
     * Clusters daily feature vectors into lifestyle archetypes.
     *
     * @param vectors List of [DailyFeatureVector] records.
     * @param k Target number of clusters (typically 3 or 4, clamped between 1 and 4).
     * @param randomSeed Seed for deterministic centroid initialization.
     * @return [LifestyleClusterResult] containing dominant archetype, percentages, and assignments.
     */
    fun cluster(
        vectors: List<DailyFeatureVector>,
        k: Int = 4,
        randomSeed: Long = 42L
    ): LifestyleClusterResult {
        if (vectors.isEmpty()) {
            return LifestyleClusterResult(
                archetype = LifestyleArchetype.SEDENTARY_MAINTENANCE,
                dominantPercentage = 0.0,
                dailyAssignments = emptyMap(),
                clusterInsights = listOf("Insufficient daily feature vectors available for lifestyle clustering.")
            )
        }

        val effectiveK = k.coerceIn(1, 4).coerceAtMost(vectors.size)
        val rawFeatures = vectors.map { it.toFeatureArray() }

        // Step 1: Min-Max feature scaling
        val (normalizedFeatures, featureMins, featureRanges) = normalizeFeatures(rawFeatures)

        // Step 2: Deterministic K-Means++ initialization
        val centroids = initializeCentroids(normalizedFeatures, effectiveK, Random(randomSeed))

        // Step 3: Lloyd's algorithm iterations
        val clusterAssignments = IntArray(vectors.size)
        var iterations = 0
        var converged = false

        while (iterations < MAX_ITERATIONS && !converged) {
            iterations++
            var changed = false

            // Assign each vector to nearest centroid
            for (i in normalizedFeatures.indices) {
                val point = normalizedFeatures[i]
                var bestCluster = 0
                var bestDistSq = Double.MAX_VALUE

                for (c in centroids.indices) {
                    val distSq = distanceSq(point, centroids[c])
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq
                        bestCluster = c
                    }
                }

                if (clusterAssignments[i] != bestCluster) {
                    clusterAssignments[i] = bestCluster
                    changed = true
                }
            }

            if (!changed) {
                converged = true
                break
            }

            // Update centroid locations
            var maxShift = 0.0
            for (c in centroids.indices) {
                val members = normalizedFeatures.filterIndexed { idx, _ -> clusterAssignments[idx] == c }
                if (members.isNotEmpty()) {
                    val newCentroid = DoubleArray(FEATURE_COUNT)
                    for (d in 0 until FEATURE_COUNT) {
                        newCentroid[d] = members.sumOf { it[d] } / members.size
                    }
                    val shift = sqrt(distanceSq(centroids[c], newCentroid))
                    if (shift > maxShift) maxShift = shift
                    centroids[c] = newCentroid
                }
            }

            if (maxShift < CONVERGENCE_EPSILON) {
                converged = true
            }
        }

        // Step 4: Calculate unnormalized centroid averages in original domain units
        val clusterProfiles = (0 until effectiveK).map { c ->
            val memberVectors = vectors.filterIndexed { idx, _ -> clusterAssignments[idx] == c }
            if (memberVectors.isNotEmpty()) {
                ClusterProfile(
                    clusterId = c,
                    memberCount = memberVectors.size,
                    avgSteps = memberVectors.map { it.steps }.average(),
                    avgSleep = memberVectors.map { it.sleepHours }.average(),
                    avgHrv = memberVectors.map { it.hrvRmssd }.average(),
                    avgCalories = memberVectors.map { it.activeCalories }.average(),
                    lateDinnerRatio = memberVectors.count { it.lateDinnerFlag }.toDouble() / memberVectors.size
                )
            } else {
                ClusterProfile(
                    clusterId = c,
                    memberCount = 0,
                    avgSteps = featureMins[0],
                    avgSleep = featureMins[1],
                    avgHrv = featureMins[2],
                    avgCalories = featureMins[3],
                    lateDinnerRatio = 0.0
                )
            }
        }

        // Step 5: Map centroids to LifestyleArchetype
        val clusterArchetypeMap = mapCentroidsToArchetypes(clusterProfiles)

        // Step 6: Map daily assignments
        val dailyAssignments = mutableMapOf<LocalDate, LifestyleArchetype>()
        for (i in vectors.indices) {
            val clusterId = clusterAssignments[i]
            val archetype = clusterArchetypeMap[clusterId] ?: LifestyleArchetype.SEDENTARY_MAINTENANCE
            dailyAssignments[vectors[i].date] = archetype
        }

        // Step 7: Determine dominant archetype and percentage
        val archetypeCounts = dailyAssignments.values.groupingBy { it }.eachCount()
        val dominantEntry = archetypeCounts.maxByOrNull { it.value }
        val dominantArchetype = dominantEntry?.key ?: LifestyleArchetype.SEDENTARY_MAINTENANCE
        val dominantPercentage = if (vectors.isNotEmpty()) {
            val rawPct = (dominantEntry?.value?.toDouble() ?: 0.0) / vectors.size * 100.0
            (rawPct * 10.0).roundToInt() / 10.0
        } else {
            0.0
        }

        // Step 8: Build insights narrative
        val clusterInsights = mutableListOf<String>()
        clusterInsights.add("Dominant lifestyle archetype: ${dominantArchetype.name} (${dominantPercentage}% of analyzed days).")
        for (profile in clusterProfiles.filter { it.memberCount > 0 }) {
            val archetype = clusterArchetypeMap[profile.clusterId] ?: LifestyleArchetype.SEDENTARY_MAINTENANCE
            val pct = (profile.memberCount.toDouble() / vectors.size * 100.0).roundToInt()
            clusterInsights.add(
                "${archetype.name} ($pct% of days, n=${profile.memberCount}): " +
                    "Avg ${profile.avgSteps.roundToInt()} steps, " +
                    "${"%.1f".format(profile.avgSleep)}h sleep, " +
                    "${profile.avgHrv.roundToInt()}ms HRV, " +
                    "${profile.avgCalories.roundToInt()} active kcal, " +
                    "late dinner ${(profile.lateDinnerRatio * 100).roundToInt()}%."
            )
        }

        return LifestyleClusterResult(
            archetype = dominantArchetype,
            dominantPercentage = dominantPercentage,
            dailyAssignments = dailyAssignments,
            clusterInsights = clusterInsights
        )
    }

    private data class ClusterProfile(
        val clusterId: Int,
        val memberCount: Int,
        val avgSteps: Double,
        val avgSleep: Double,
        val avgHrv: Double,
        val avgCalories: Double,
        val lateDinnerRatio: Double
    )

    private fun mapCentroidsToArchetypes(profiles: List<ClusterProfile>): Map<Int, LifestyleArchetype> {
        val activeProfiles = profiles.filter { it.memberCount > 0 }
        if (activeProfiles.isEmpty()) return emptyMap()

        if (activeProfiles.size == 1) {
            return mapOf(activeProfiles.first().clusterId to LifestyleArchetype.SEDENTARY_MAINTENANCE)
        }

        // Normalize profile metrics among centroids (0.0 to 1.0)
        fun relativeScore(extract: (ClusterProfile) -> Double): Map<Int, Double> {
            val values = activeProfiles.map { extract(it) }
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 1.0
            val range = max - min
            return activeProfiles.associate {
                it.clusterId to (if (range > 1e-6) (extract(it) - min) / range else 0.5)
            }
        }

        val normSteps = relativeScore { it.avgSteps }
        val normSleep = relativeScore { it.avgSleep }
        val normHrv = relativeScore { it.avgHrv }
        val normCalories = relativeScore { it.avgCalories }
        val normDinner = relativeScore { it.lateDinnerRatio }

        // Archetype heuristic scores:
        // Peak Performance: highest calories + steps
        // Deep Recovery: highest sleep + HRV
        // Strain Depleted: lowest sleep + HRV with elevated late dinner
        // Sedentary Maintenance: lower exertion with baseline sleep
        val exertionScores = activeProfiles.associate { p ->
            val id = p.clusterId
            id to ((normSteps[id] ?: 0.0) + (normCalories[id] ?: 0.0))
        }

        val recoveryScores = activeProfiles.associate { p ->
            val id = p.clusterId
            id to ((normSleep[id] ?: 0.0) + (normHrv[id] ?: 0.0))
        }

        val strainScores = activeProfiles.associate { p ->
            val id = p.clusterId
            id to ((1.0 - (normSleep[id] ?: 0.0)) + (1.0 - (normHrv[id] ?: 0.0)) + (normDinner[id] ?: 0.0))
        }

        val sedentaryScores = activeProfiles.associate { p ->
            val id = p.clusterId
            id to ((1.0 - (normSteps[id] ?: 0.0)) + (1.0 - (normCalories[id] ?: 0.0)))
        }

        val assigned = mutableMapOf<Int, LifestyleArchetype>()
        val unassigned = activeProfiles.map { it.clusterId }.toMutableSet()

        // 1. Assign Peak Performance (highest exertion: calories + steps)
        val peakId = unassigned.maxByOrNull { exertionScores[it] ?: 0.0 }
        if (peakId != null) {
            assigned[peakId] = LifestyleArchetype.PEAK_PERFORMANCE
            unassigned.remove(peakId)
        }

        // 2. Assign Deep Recovery (highest sleep + HRV among remaining)
        val recoveryId = unassigned.maxByOrNull { recoveryScores[it] ?: 0.0 }
        if (recoveryId != null) {
            assigned[recoveryId] = LifestyleArchetype.DEEP_RECOVERY
            unassigned.remove(recoveryId)
        }

        // 3. For remaining clusters
        if (unassigned.size == 2) {
            val list = unassigned.toList()
            val strain0 = strainScores[list[0]] ?: 0.0
            val strain1 = strainScores[list[1]] ?: 0.0
            if (strain0 >= strain1) {
                assigned[list[0]] = LifestyleArchetype.STRAIN_DEPLETED
                assigned[list[1]] = LifestyleArchetype.SEDENTARY_MAINTENANCE
            } else {
                assigned[list[1]] = LifestyleArchetype.STRAIN_DEPLETED
                assigned[list[0]] = LifestyleArchetype.SEDENTARY_MAINTENANCE
            }
        } else if (unassigned.size == 1) {
            val remainingId = unassigned.first()
            val strain = strainScores[remainingId] ?: 0.0
            val sed = sedentaryScores[remainingId] ?: 0.0
            assigned[remainingId] = if (strain > sed) {
                LifestyleArchetype.STRAIN_DEPLETED
            } else {
                LifestyleArchetype.SEDENTARY_MAINTENANCE
            }
        }

        return assigned
    }

    private fun normalizeFeatures(features: List<DoubleArray>): Triple<List<DoubleArray>, DoubleArray, DoubleArray> {
        val mins = DoubleArray(FEATURE_COUNT) { d -> features.minOf { it[d] } }
        val maxs = DoubleArray(FEATURE_COUNT) { d -> features.maxOf { it[d] } }
        val ranges = DoubleArray(FEATURE_COUNT) { d ->
            val diff = maxs[d] - mins[d]
            if (diff > 1e-6) diff else 1.0
        }

        val normalized = features.map { point ->
            DoubleArray(FEATURE_COUNT) { d ->
                (point[d] - mins[d]) / ranges[d]
            }
        }

        return Triple(normalized, mins, ranges)
    }

    private fun initializeCentroids(
        points: List<DoubleArray>,
        k: Int,
        random: Random
    ): Array<DoubleArray> {
        val centroids = mutableListOf<DoubleArray>()

        // Pick first centroid uniformly at random
        val firstIndex = random.nextInt(points.size)
        centroids.add(points[firstIndex].clone())

        // Pick subsequent centroids via K-Means++ distance-weighted probability
        while (centroids.size < k) {
            val distSqArray = DoubleArray(points.size) { i ->
                centroids.minOf { c -> distanceSq(points[i], c) }
            }
            val totalDistSq = distSqArray.sum()

            if (totalDistSq <= 1e-9) {
                // All points identical or co-located with centroids
                val remainingIndex = points.indices.firstOrNull { idx ->
                    centroids.none { c -> distanceSq(points[idx], c) < 1e-9 }
                } ?: random.nextInt(points.size)
                centroids.add(points[remainingIndex].clone())
            } else {
                val target = random.nextDouble() * totalDistSq
                var cumulative = 0.0
                var selected = points.lastIndex
                for (i in distSqArray.indices) {
                    cumulative += distSqArray[i]
                    if (cumulative >= target) {
                        selected = i
                        break
                    }
                }
                centroids.add(points[selected].clone())
            }
        }

        return centroids.toTypedArray()
    }

    private fun distanceSq(a: DoubleArray, b: DoubleArray): Double {
        var sum = 0.0
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sum
    }
}
