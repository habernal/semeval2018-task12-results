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

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivan Habernal
 */
public class Main
{

    static DecimalFormat df;

    static {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        df = (DecimalFormat) nf;
        df.applyPattern("#.00");
    }

    /**
     * Prints the final rank of all participants, ordered by accuracy.
     *
     * @param participants participants
     * @param goldData     gold data
     * @param latex
     */
    public static void finalRank(List<Participant> participants, Map<String, Integer> goldData,
            boolean latex)
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

        if (latex) {
            System.out.println("\\begin{tabular}{rlr}");
            System.out
                    .println("\\textbf{Rank} & \\textbf{System} & \\textbf{Accuracy} \\\\ \\hline");
            printResultTable(entries, "%s & %s & %.3f \\\\%n", participant -> {
                String name = participant.getShownName().replace("_", "\\_");
                if (participant.getNoResponse()) {
                    return name + "*";
                }
                else {
                    return name;
                }
            });
            System.out.println("\\end{tabular}");
        }
        else {
            printResultTable(entries, "%s %s  %.3f%n", Participant::getShownName);
        }
    }

    private static void printResultTable(List<Map.Entry<Double, List<Participant>>> entries,
            String lineFormat, Function<Participant, String> participantPrinter)
    {
        int rank = 1;
        for (Map.Entry<Double, List<Participant>> entry : entries) {
            List<Participant> participantsWithSameAccuracy = entry.getValue();

            for (Participant p : participantsWithSameAccuracy) {
                System.out.printf(Locale.ENGLISH, lineFormat,
                        StringUtils.leftPad(String.valueOf(rank), 2),
                        StringUtils.leftPad(participantPrinter.apply(p), 16), entry.getKey());
            }

            rank += participantsWithSameAccuracy.size();
        }
    }

    /**
     * For each participants, shows the number of correct and incorrect guesses
     *
     * @param participants participants
     * @param goldData     gold data
     */
    public static void printSuccessFailureCounts(List<Participant> participants,
            Map<String, Integer> goldData)
    {
        participants.forEach(p -> {
            SortedMap<String, Boolean> evaluatePredictions = Scorer
                    .evaluatePredictions(goldData, p.getPredictions());
            long success = evaluatePredictions.values().stream().filter(val -> val).count();
            long failure = evaluatePredictions.values().stream().filter(val -> !val).count();

            System.out.printf(Locale.ENGLISH, "%s\t%d\t%d%n",
                    StringUtils.leftPad(p.getShownName(), 16), success, failure);
        });
    }

    /**
     * Shows problematic instances (distributions of successes)
     *
     * @param participants participants
     * @param goldData     gold data
     */
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

        // counts by successful systems
        List<Integer> counts = list.stream().map(Map.Entry::getValue).collect(Collectors.toList());
//        Map<Integer, Integer> histogram = new TreeMap<>();
//        counts.forEach(i -> {
//            histogram.put(i, histogram.getOrDefault(i, 0) + 1);
//        });


        System.out.println("Run this in R-Studio to get the histogram");
//        System.out.println("a = c(" + histogram.values().stream().map(Object::toString).collect(Collectors.joining(", ")) + ")");
        System.out.println("counts <- table(c(" + counts.stream().map(Object::toString).collect(Collectors.joining(", ")) + "))");
        System.out.println("barplot(counts, xlab=\"Number of systems that solved a particular instance\", ylab=\"Correct instances\", main=\"Distribution of correctly classified instances among all systems\", cex.lab=1.5, cex.main=1.3)");

    }

    /**
     * Runs and prints Approximate Randomization Test
     *
     * @param participants participants
     * @param goldData     gold data
     */
    private static void runApproximateRandomizationTest(List<Participant> participants,
            SortedMap<String, Integer> goldData)
    {
        final Comparator<Participant> ACCURACY_BASED_COMPARATOR = new Comparator<Participant>()
        {
            @Override
            public int compare(Participant o1, Participant o2)
            {
                double accuracyP1 = Scorer
                        .computeAccuracy(Scorer.evaluatePredictions(goldData, o1.getPredictions()));
                double accuracyP2 = Scorer
                        .computeAccuracy(Scorer.evaluatePredictions(goldData, o2.getPredictions()));

                int diff = Double.compare(accuracyP2, accuracyP1);

                // or alphabetically, if accuracy is the same
                if (diff == 0) {
                    return o1.getShownName().compareTo(o2.getShownName());
                }

                return diff;
            }
        };

        Table<Participant, Participant, Double> pValueTable = TreeBasedTable
                .create(ACCURACY_BASED_COMPARATOR, ACCURACY_BASED_COMPARATOR);

        List<Participant> sortedParticipants = new ArrayList<>(participants);
        sortedParticipants.sort(ACCURACY_BASED_COMPARATOR);

        for (int i = 0; i < sortedParticipants.size(); i++) {
            for (int j = 0; j <= i; j++) {

                Participant p1 = sortedParticipants.get(i);
                Participant p2 = sortedParticipants.get(j);

                // accuracy on the diagonal
                if (i == j) {
                    pValueTable.put(p1, p2, Scorer.computeAccuracy(
                            Scorer.evaluatePredictions(p1.getPredictions(), goldData)));
                }
                else {

                    double pValue = ApproximateRandomizationTest
                            .pValue(p1.getPredictions(), p2.getPredictions(), goldData);
                    System.out.printf(Locale.ENGLISH, "%s\t%s\t%.4f%n", p1.getShownName(),
                            p2.getShownName(), pValue);

                    pValueTable.put(p1, p2, pValue);
                }
            }
        }

        // LaTeX table header
        int numberOfColumns = pValueTable.columnKeySet().size();
        System.out.println("\\begin{tabular*}{\\linewidth}{" +
                // generate latex column description (number of "l"'s)
                Stream.generate(() -> "l").limit(numberOfColumns + 1)
                        .collect(Collectors.joining("")) + "}\n\\toprule");

        String header = pValueTable.columnKeySet().stream()
                .map(p -> "\\rotatebox{90}{" + p.getShownNameLaTeX() + "}")
                .collect(Collectors.joining(" & "));
        System.out.println(" & " + header + " \\\\");

        pValueTable.rowMap().forEach((participant, participantDoubleMap) -> {
            System.out.print(participant.getShownNameLaTeX() + " & ");
            List<String> cells = participantDoubleMap.entrySet().stream()
                    .map(participantDoubleEntry -> {
                        Participant column = participantDoubleEntry.getKey();
                        Double value = participantDoubleEntry.getValue();

                        if (column.getShownName().equals(participant.getShownName())) {
                            // accuracy
                            return "\\textbf{" + df.format(value) + "}";
                        }
                        else {
                            // p-value
                            return df.format(value);
                        }
                    }).collect(Collectors.toList());

            // we need to add empty cells too
            for (int i = cells.size(); i < numberOfColumns; i++) {
                cells.add(" ");
            }

            System.out.print(cells.stream().collect(Collectors.joining(" & ")));

            //            System.out.println(participantDoubleMap.values().stream()
            //                    .map(d -> df.format(d))
            //                    .collect(Collectors.joining("\t")));
            System.out.println(" \\\\");
        });

        System.out.println("\\end{tabular*}");
    }

    public static void main(String[] args)
            throws Exception
    {
        // all participants with submissions
        List<Participant> participants = ParticipantManager
                .loadParticipants(new File("data/submissions/metadata-public.xml"));

        // gold labels
        SortedMap<String, Integer> goldData = Scorer
                .readLabelsFromFile(new File("data/gold/truth.txt"));

        // plain results
        finalRank(participants, goldData, false);
        // latex
        finalRank(participants, goldData, true);

                //        printSuccessFailureCounts(participants, goldData);

        List<Participant> participantsForComparison = participants.stream()
                .filter(p -> !p.isWithdrawn() && !p.isNoResponse()).collect(Collectors.toList());
        /*
        runApproximateRandomizationTest(participantsForComparison, goldData);
        */

        problematicInstanceDistribution(participantsForComparison, goldData);
    }

}
