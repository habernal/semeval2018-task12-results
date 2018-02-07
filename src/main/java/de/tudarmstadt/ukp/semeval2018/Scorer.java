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

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Official scorer for SemEval 2018 Task 12 - The Argument Reasoning Comprehension Task.
 *
 * @author Ivan Habernal
 */
public class Scorer
{
    private static final Charset UTF8 = Charset.forName("utf-8");

    /**
     * Reads a map of (instanceID, gold label) from a text file. Each instance is on a separate
     * line, starting with instance ID, then whitespace (space or tabs) and then 0 or 1 (the gold
     * label)
     *
     * @param file file
     * @return a map (never null)
     */
    public static SortedMap<String, Integer> readLabelsFromFile(File file)
            throws IOException
    {
        FileInputStream inputStream = new FileInputStream(file);
        List<String> lines = IOUtils.readLines(inputStream, UTF8);
        IOUtils.closeQuietly(inputStream);

        SortedMap<String, Integer> result = new TreeMap<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // only for non-empty lines and non-comments
            if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                String[] split = line.split("\\s+");

                if (split.length != 2) {
                    throw new IllegalArgumentException("Error on line " + (i + 1)
                            + ", expected two whitespace-delimited entries but got '" + line
                            + "' (file "
                            + file.getAbsolutePath() + ")");
                }

                String id = split[0].trim();
                int value = -1;

                try {
                    value = Integer.valueOf(split[1]);
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Error on line " + (i + 1)
                            + ", expected an integer but got '" + split[1] + "' (file "
                            + file.getAbsolutePath() + ")");
                }

                if (!(value == 0 || value == 1)) {
                    throw new IllegalArgumentException("Error on line " + (i + 1)
                            + ", expected 0 or 1 but got '" + split[1] + "' (file "
                            + file.getAbsolutePath() + ")");
                }

                result.put(id, value);
            }
        }

        return result;
    }

    public static double computeAccuracy(SortedMap<String, Boolean> evaluatedPredictions)
            throws IllegalArgumentException
    {
        double correct = evaluatedPredictions.values().stream().filter(v -> v).count();
        double incorrect = evaluatedPredictions.values().stream().filter(v -> !v).count();

        if (correct + incorrect != evaluatedPredictions.size()) {
            throw new IllegalStateException("Predictions inconsistent");
        }

        return correct / (correct + incorrect);
    }

    /**
     * Evaluates predictions against gold data. Returns a map where the key is the instance
     * ID and the value is true if the gold label matches the prediction (true positive),
     * false otherwise.
     *
     * @param gold        map of gold labels
     * @param predictions map of predicted labels
     * @return map of true/false successes
     * @throws IllegalArgumentException in case of data inconsistencies
     */
    public static SortedMap<String, Boolean> evaluatePredictions(Map<String, Integer> gold,
            Map<String, Integer> predictions)
            throws IllegalArgumentException
    {
        if (gold == null) {
            throw new IllegalArgumentException("Parameter 'gold' is null");
        }

        if (predictions == null) {
            throw new IllegalArgumentException("Parameter 'predictions' is null");
        }

        if (gold.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'gold' is an empty map");
        }

        if (predictions.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'predictions' is an empty map");
        }

        if (!(gold.keySet().containsAll(predictions.keySet()) && predictions.keySet()
                .containsAll(gold.keySet()))) {
            throw new IllegalArgumentException(
                    "Gold set and predictions contain different instance IDs");
        }

        SortedMap<String, Boolean> result = new TreeMap<>();

        for (String id : gold.keySet()) {
            int goldLabel = gold.get(id);
            int predictedLabel = predictions.get(id);

            result.put(id, goldLabel == predictedLabel);
        }

        return result;
    }
}
