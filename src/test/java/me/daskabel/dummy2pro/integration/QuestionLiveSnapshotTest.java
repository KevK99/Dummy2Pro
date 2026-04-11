package me.daskabel.dummy2pro.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionLiveSnapshotTest
{
    @Test
    void liveDatabaseMustMatchCommittedSnapshot() throws Exception
    {
        String currentSnapshot = LiveQuestionDatasetSupport.buildSnapshot(
                LiveQuestionDatasetSupport.loadQuestions()
        );

        if (Boolean.getBoolean("dbcheck.updateSnapshot"))
        {
            LiveQuestionDatasetSupport.writeManagedSnapshot(currentSnapshot);
            return;
        }

        String expectedSnapshot = LiveQuestionDatasetSupport.readManagedSnapshot();

        String normalizedCurrent = LiveQuestionDatasetSupport.normalize(currentSnapshot);
        String normalizedExpected = LiveQuestionDatasetSupport.normalize(expectedSnapshot);

        if (!normalizedExpected.equals(normalizedCurrent))
        {
            LiveQuestionDatasetSupport.writeActualSnapshot(currentSnapshot);
        }

        assertEquals(
                normalizedExpected,
                normalizedCurrent,
                "Der Fragen-Snapshot stimmt nicht mehr mit der echten Datenbank überein. "
                        + "Die aktuelle Ausgabe steht in "
                        + LiveQuestionDatasetSupport.actualSnapshotPath()
        );
    }
}