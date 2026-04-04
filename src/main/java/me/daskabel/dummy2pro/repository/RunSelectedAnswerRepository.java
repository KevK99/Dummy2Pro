package me.daskabel.dummy2pro.repository;

import me.daskabel.dummy2pro.model.RunSelectedAnswer;
import me.daskabel.dummy2pro.model.RunSelectedAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RunSelectedAnswerRepository extends JpaRepository<RunSelectedAnswer, RunSelectedAnswerId> {
    List<RunSelectedAnswer> findByRun_RunId(Long runId);

    List<RunSelectedAnswer> findByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    Optional<RunSelectedAnswer> findByRun_RunIdAndQuestion_QuestionIdAndAnswerOption_AnswerId(
            Long runId,
            Long questionId,
            Long answerId
    );

    void deleteByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);

    void deleteByRun_RunId(Long runId);

    long countByRun_RunIdAndQuestion_QuestionId(Long runId, Long questionId);
}
