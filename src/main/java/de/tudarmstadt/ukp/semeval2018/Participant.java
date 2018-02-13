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

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;

/**
 * @author Ivan Habernal
 */
public class Participant
{
    private final String userName;
    private final String submissionID;
    private boolean withdrawn = false;

    private String shownName;

    private String systemName;

    private SortedMap<String, Integer> predictions;

    public Participant(String lineFromMetadataFile)
            throws IllegalArgumentException
    {
        String[] split = lineFromMetadataFile.split("\t");

        // userName submissionID
        this.userName = split[0].trim();
        this.submissionID = split[1].trim();
    }

    public Participant(String userName, String submissionID)
    {
        this.userName = userName;
        this.submissionID = submissionID;
    }

    public void loadSubmission(File submissionMainDirectory)
    {
        try {
            this.predictions = Scorer
                    .readLabelsFromFile(
                            new File(submissionMainDirectory, this.submissionID + ".txt"));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserName()
    {
        return userName;
    }

    public String getSubmissionID()
    {
        return submissionID;
    }

    public SortedMap<String, Integer> getPredictions()
    {
        return predictions;
    }

    @Override
    public String toString()
    {
        return "Participant{" + "userName='" + userName + '\'' + ", submissionID='" + submissionID
                + '\'' + ", withdrawn=" + withdrawn + ", shownName='" + shownName + '\''
                + ", systemName='" + systemName + '\'' + '}';
    }

    public boolean isWithdrawn()
    {
        return withdrawn;
    }

    public void setWithdrawn(boolean withdrawn)
    {
        this.withdrawn = withdrawn;
    }

    public String getSystemName()
    {
        return systemName;
    }

    public void setSystemName(String systemName)
    {
        this.systemName = systemName;
    }

    public String getShownName()
    {
        return shownName;
    }

    public void setShownName(String shownName)
    {
        this.shownName = shownName;
    }
}
