package me.daskabel.dummy2pro.repository;

import me.daskabel.dummy2pro.model.RunGapAnswer;
import me.daskabel.dummy2pro.model.RunGapAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RunGapAnswerRepository extends JpaRepository<RunGapAnswer, RunGapAnswerId>
{
    List<RunGapAnswer> findByRun_RunId(Long runId);

    List<RunGapAnswer> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    Optional<RunGapAnswer> findByRun_RunIdAndQuestion_QuestionIdAndGapField_GapId(
            Long runId,
            Long questionId,
            Long gapId
    );

    void deleteByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);
}