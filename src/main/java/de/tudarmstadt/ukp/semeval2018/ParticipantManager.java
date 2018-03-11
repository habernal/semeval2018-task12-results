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

import nu.xom.*;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ivan Habernal
 */
public class ParticipantManager
{
    public static List<Participant> loadParticipants(File metadataFile)
            throws IOException
    {
        List<Participant> participants = new ArrayList<>();

        try {

            Builder builder = new Builder();
            Document document = builder.build(metadataFile);

            Elements participantEls = document.getRootElement().getChildElements("participant");
            for (int i = 0; i < participantEls.size(); i++) {
                Element el = participantEls.get(i);

                String username = el.getAttributeValue("username");
                String submissionID = el.getAttributeValue("submissionID");
                String systemName = el.getAttributeValue("systemName");
                boolean noResponse = "true".equals(el.getAttributeValue("noResponse"));
                boolean withdrawn = "true".equals(el.getAttributeValue("withdrawn"));
                boolean isBaseline = "true".equals(el.getAttributeValue("baseline"));

                Participant participant = new Participant(username, submissionID);
                participant.setWithdrawn(noResponse);
                participant.setNoResponse(noResponse);
                participant.setSystemName(systemName);
                participant.setWithdrawn(withdrawn);
                participant.setBaseline(isBaseline);

                String shownName = el.getAttributeValue("shownName");
                if (shownName != null) {
                    participant.setShownName(shownName);
                }
                else {
                    participant.setShownName(participant.getSystemName());
                }

                // no system name or shown name -> fallback to username
                if (StringUtils.isBlank(participant.getShownName())) {
                    participant.setShownName(participant.getUserName());
                }

                participants.add(participant);

                System.out.println(participant);
            }

        }
        catch (ParsingException e) {
            throw new IOException(e);
        }

        participants
                .forEach(participant -> participant.loadSubmission(metadataFile.getParentFile()));

        // remove withdrawn submissions
        participants = participants.stream().filter(participant -> !participant.isWithdrawn())
                .collect(Collectors.toList());

        return participants;
    }
}
