/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.semeval2018;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Implementation of Approximate Randomization Test as described in
 *
 * Riezler, S., & Maxwell, J. T. (2005). On Some Pitfalls in Automatic Evaluation and
 * Significance Testing for MT. In Proceedings of ACL Workshop on Intrinsic and Extrinsic
 * Evaluation Measures for Machine Translation and/or Summarization (pp. 57–-64). Ann Arbor,
 * Michigan: Association for Computational Linguistics. http://www.aclweb.org/anthology/W05-0908
 *
 * @author Ivan Habernal
 */
public class ApproximateRandomizationTest
{
    private static final Random RANDOM = new Random(12345);

    private static List<SortedMap<String, Integer>> swapRandomValues(SortedMap<String, Integer> system1,
            SortedMap<String, Integer> system2)
            throws IllegalArgumentException
    {
        SortedMap<String, Integer> system1Output = new TreeMap<>();
        SortedMap<String, Integer> system2Output = new TreeMap<>();

        for (String key : system1.keySet()) {
            if (RANDOM.nextBoolean()) {
                system1Output.put(key, system1.get(key));
                system2Output.put(key, system2.get(key));
            }
            else {
                // or swap
                system1Output.put(key, system2.get(key));
                system2Output.put(key, system1.get(key));
            }
        }

        return Arrays.asList(system1Output, system2Output);
    }

    public static double pValue(SortedMap<String, Integer> system1,
            SortedMap<String, Integer> system2, SortedMap<String, Integer> goldData)
    {
        double s_X = Scorer.computeAccuracy(Scorer.evaluatePredictions(system1, goldData));
        double s_Y = Scorer.computeAccuracy(Scorer.evaluatePredictions(system2, goldData));

        final int RANDOM_SHUFFLES = 10000;

        int c = IntStream.range(0, RANDOM_SHUFFLES).parallel().map(operand -> {
            List<SortedMap<String, Integer>> swapped = swapRandomValues(system1, system2);
            SortedMap<String, Integer> system1Shuffled = swapped.get(0);
            SortedMap<String, Integer> system2Shuffled = swapped.get(1);

            double s_X_r = Scorer
                    .computeAccuracy(Scorer.evaluatePredictions(system1Shuffled, goldData));
            double s_Y_r = Scorer
                    .computeAccuracy(Scorer.evaluatePredictions(system2Shuffled, goldData));

            if (Math.abs(s_X_r - s_Y_r) >= Math.abs(s_X - s_Y)) {
                return 1;
            }

            return 0;
        }).sum();

        return ((double) c + 1.0) / (RANDOM_SHUFFLES + 1.0);
    }
}
