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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ivan Habernal
 */
public class Main
{
    public static void generateListOfAccuracies(List<Participant> participants,
            Map<String, Integer> goldData)
    {

        for (Participant p : participants) {
            System.out.printf("%s\t", p.getUserName());
            for (int r = 1; r < 1000; r++) {

                List<String> goldIDs = new ArrayList<>(goldData.keySet());
                Collections.shuffle(goldIDs, new Random(r));
                // split gold data into 10 buckets
                int bucketSize = (int) Math.ceil((double) goldIDs.size() / (double) 10);
                List<List<String>> buckets = ListUtils.partition(goldIDs, bucketSize);

                SortedMap<String, Integer> predictions = p.getPredictions();

                for (List<String> bucket : buckets) {
                    int bucketSuccess = 0;
                    int bucketFailures = 0;
                    for (String id : bucket) {
                        boolean success = predictions.get(id).equals(goldData.get(id));

                        if (success) {
                            bucketSuccess++;
                        }
                        else {
                            bucketFailures++;
                        }
                    }

                    double bucketAccuracy =
                            (double) bucketSuccess / ((double) bucketSuccess + bucketFailures);
                    System.out.printf(Locale.ENGLISH, "%.3f, ", bucketAccuracy);
                }
            }
                System.out.println();
        }

    }

    public static void finalRank(List<Participant> participants, Map<String, Integer> goldData)
    {
        SortedMap<Double, List<Participant>> accuracies = new TreeMap<>();

        // evaluate predictions
        for (Participant p : participants) {
            SortedMap<String, Boolean> evaluatePredictions = Scorer
                    .evaluatePredictions(goldData, p.getPredictions());

            // compute accuracy
            double accuracy = Scorer.computeAccuracy(evaluatePredictions);

            accuracies.putIfAbsent(accuracy, new ArrayList<>());
            accuracies.get(accuracy).add(p);
        }

        // print
        List<Map.Entry<Double, List<Participant>>> entries = new ArrayList<>(accuracies.entrySet());
        Collections.reverse(entries);
        int rank = 1;
        for (Map.Entry<Double, List<Participant>> entry : entries) {
            List<Participant> participantsWithSameAccuracy = entry.getValue();

            for (Participant p : participantsWithSameAccuracy) {
                System.out.printf(Locale.ENGLISH, "%s %s  %.2f%n",
                        StringUtils.leftPad(String.valueOf(rank), 2),
                        StringUtils.leftPad(p.getShownName(), 16),
                        entry.getKey());
            }

            rank += participantsWithSameAccuracy.size();
        }
    }

    public static void printSuccessFailureCounts(List<Participant> participants,
            Map<String, Integer> goldData)
    {
        participants.forEach(p -> {
            SortedMap<String, Boolean> evaluatePredictions = Scorer
                    .evaluatePredictions(goldData, p.getPredictions());
            long success = evaluatePredictions.values().stream().filter(val -> val).count();
            long failure = evaluatePredictions.values().stream().filter(val -> !val).count();

            System.out.printf(Locale.ENGLISH, "%s\t%d\t%d%n",
                    StringUtils.leftPad(p.getUserName(), 16),
                    success, failure);
        });
    }

    public static void main(String[] args)
            throws Exception
    {
        // all participants with submissions
        List<Participant> participants = ParticipantManager
                .loadParticipants(new File("data/submissions/metadata.xml"), false);

        // gold labels
        Map<String, Integer> goldData = Scorer.readLabelsFromFile(new File("data/gold/truth.txt"));

        finalRank(participants, goldData);

        // remove random baseline
        List<Participant> trueParticipants = participants.stream()
                .filter(p -> !p.getUserName().equals("RANDOM_BASELINE")).collect(
                        Collectors.toList());

        //        problematicInstanceDistribution(trueParticipants, goldData);

//        printSuccessFailureCounts(participants, goldData);

//        generateListOfAccuracies(participants, goldData);
    }

    private static void problematicInstanceDistribution(List<Participant> participants,
            Map<String, Integer> goldData)
    {
        SortedMap<String, Integer> correctCounts = new TreeMap<>();

        // evaluate predictions
        for (Participant p : participants) {
            SortedMap<String, Boolean> evaluatePredictions = Scorer
                    .evaluatePredictions(goldData, p.getPredictions());

            evaluatePredictions.forEach((instanceID, correct) -> {
                correctCounts.putIfAbsent(instanceID, 0);
                if (correct) {
                    correctCounts.put(instanceID, correctCounts.get(instanceID) + 1);
                }
            });
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(correctCounts.entrySet());
        list.sort(Comparator.comparing(Map.Entry::getValue));

        System.out.println(list);
        System.out.println(
                list.stream().map(stringIntegerEntry -> stringIntegerEntry.getValue().toString())
                        .collect(Collectors.joining(", ")));

    }
}
